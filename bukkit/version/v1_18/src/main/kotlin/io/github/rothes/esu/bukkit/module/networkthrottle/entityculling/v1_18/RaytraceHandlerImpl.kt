package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.v1_18

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.CullDataManager
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerVelocityGetter
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.RaytraceHandler
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.UserCullData
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.version.VersionUtils.versioned
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter.Companion.connected
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.bukkit.util.version.adapter.nms.*
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.UnsafeUtils.usBooleanAccessor
import io.github.rothes.esu.core.util.extension.math.floorI
import io.github.rothes.esu.core.util.extension.math.frac
import io.github.rothes.esu.core.util.extension.math.square
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import kotlinx.coroutines.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.spigotmc.TrackingRange
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

object RaytraceHandlerImpl: RaytraceHandler<RaytraceHandlerImpl.RaytraceConfig, EmptyConfiguration>() {

    private const val COLLISION_EPSILON = 1E-7
    private val INIT_SECTION: Array<LevelChunkSection> = arrayOf()
    private val ENTITY_TYPES: Int

    init {
        val registryAccessHandler = MCRegistryAccessHandler::class.java.versioned()
        val registries = MCRegistries::class.java.versioned()
        val registry = registryAccessHandler.getRegistryOrThrow(
            registryAccessHandler.getServerRegistryAccess(),
            registries.entityType
        )
        ENTITY_TYPES = registryAccessHandler.size(registry)
    }

    private val levelEntitiesHandler by Versioned(LevelEntitiesHandler::class.java)
    private val playerVelocityGetter by Versioned(PlayerVelocityGetter::class.java)
    private val entityHandleGetter by Versioned(EntityHandleGetter::class.java)

    private val shapedOcclusion = BlockBehaviour.BlockStateBase::class.java.getDeclaredField("useShapeForLightOcclusion").usBooleanAccessor
    private val canOcclude = BlockBehaviour.BlockStateBase::class.java.getDeclaredField("canOcclude").usBooleanAccessor

    private var raytracer: RayTracer = StepRayTracer
    private var forceVisibleDistanceSquared = 0.0
    private var millisBetweenUpdates = 50

    private var lastThreads = 0
    private var coroutine: ExecutorCoroutineDispatcher? = null

    private var previousElapsedTime = 0L
    private var previousDelayTime = 0L

    private val removedEntities = IntArrayList()

    override fun checkConfig(): Feature.AvailableCheck? {
        if (config.raytraceThreads < 1) {
            plugin.err("[EntityCulling] At least one raytrace thread is required to enable this feature.")
            return Feature.AvailableCheck.fail { "At least one raytrace thread is required!".message }
        }
        return null
    }

    override fun onEntityRemove(entity: org.bukkit.entity.Entity) {
        synchronized(removedEntities) {
            removedEntities.add(entityHandleGetter.getHandle(entity).id)
        }
    }

    override fun isValid(bukkitEntity: org.bukkit.entity.Entity): Boolean {
        val handle = entityHandleGetter.getHandle(bukkitEntity)
        return handle.isAlive && handle.valid
    }

    override fun onReload() {
        super.onReload()
        forceVisibleDistanceSquared = config.forceVisibleDistance * config.forceVisibleDistance
        millisBetweenUpdates = 1000 / config.updatesPerSecond
        if (enabled) {
            init()
        }
    }

    override fun onEnable() {
        init()
        registerCommands(Commands)
    }

    override fun onDisable() {
        super.onDisable()
        coroutine?.close()
        coroutine = null
        lastThreads = 0
        EntitySummonListener.unregister()
    }

    private fun init() {
        val config = config
        if (lastThreads != config.raytraceThreads)
            startThread()
        raytracer = if (config.fastRaytrace) StepRayTracer else DDARayTracer
        if (config.entityCulledByDefault)
            EntitySummonListener.register()
        else
            EntitySummonListener.unregister()
    }

    private fun startThread() {
        coroutine?.close()
        val nThreads = config.raytraceThreads

        val name = "ESU-EntityCulling"
        val threadNo = AtomicInteger()
        val executor = Executors.newScheduledThreadPool(nThreads) { runnable ->
            Thread(runnable, if (nThreads == 1) name else name + "-" + threadNo.incrementAndGet()).apply {
                priority = Thread.NORM_PRIORITY - 1
                isDaemon = true
            }
        }
        val context = Executors.unconfigurableExecutorService(executor).asCoroutineDispatcher()
        CoroutineScope(context).launch {
            while (isActive) {
                val millis = System.currentTimeMillis()
                val removedEntities = synchronized(removedEntities) {
                    val copy = IntArrayList(removedEntities)
                    removedEntities.clear()
                    copy
                }
                Bukkit.getWorlds().flatMapTo(
                    ArrayList(Bukkit.getOnlinePlayers().size + 1)
                ) { bukkitWorld ->
                    val level = (bukkitWorld as CraftWorld).handle
                    val players = level.players()
                    if (players.isEmpty()) return@flatMapTo emptyList()
                    // `level.entityLookup.all` + distance check is already the fastest way to collect all entities to check.
                    // Get regions from entityLookup, then loop over each chunk to collect entities is 2x slower.
                    val entitiesRaw: Iterable<Entity?> = levelEntitiesHandler.getEntitiesAll(level) // May collect null entity on Paper 1.20.1

                    /* Sort entities by tracking range */
                    val entityTypeMap = Reference2ReferenceOpenHashMap<EntityType<*>, MutableList<Entity>>(ENTITY_TYPES)
                    for (entity in entitiesRaw) {
                        entity ?: continue
                        val get = entityTypeMap.get(entity.type)
                        if (get != null) get.add(entity)
                        else entityTypeMap[entity.type] = ArrayList<Entity>(32).also { it.add(entity) }
                    }
                    val entityMap = Int2ReferenceOpenHashMap<MutableList<Entity>>(ENTITY_TYPES)
                    for ((type, list) in entityTypeMap) {
                        val vanillaRange = type.clientTrackingRange() shl 4
                        val range = TrackingRange.getEntityTrackingRange(list[0], vanillaRange)
                        val get = entityMap.get(range)
                        if (get != null)
                            get.addAll(list)
                        else
                            entityMap.put(range, list)
                    }
                    val entities = entityMap.int2ReferenceEntrySet().map {
                        val squaredRange = (it.intKey + 8).square() // Add extra 8 blocks
                        SortedEntities(squaredRange, it.value)
                    }
                    /* Sort entities by tracking range */

                    players.map { player ->
                        launch {
                            val bukkit = player.bukkitEntity
                            if (!bukkit.connected) return@launch // Player may disconnect at the same time
                            try {
                                val data = CullDataManager[bukkit]
                                data.onEntityRemove(removedEntities)
                                tickPlayer(player, bukkit, data, level, entities)
                            } catch (e: Throwable) {
                                plugin.err("[EntityCulling] Failed to update player ${bukkit.name}", e)
                            }
                        }
                    }
                }.joinAll()
                val elapsed = System.currentTimeMillis() - millis
                val delay = (millisBetweenUpdates - elapsed).coerceAtLeast(1)
                previousElapsedTime = elapsed
                previousDelayTime = delay
                delay(delay)
            }
        }
        lastThreads = nThreads
        coroutine = context
    }

    object EntitySummonListener : Listener {

        private val levelHandler by Versioned(LevelHandler::class.java)

        @EventHandler
        fun onEntitySpawn(event: EntitySpawnEvent) {
            val entity = (event.entity as CraftEntity).handle
            val level = levelHandler.level(entity) as? ServerLevel ?: return
            for (player in level.players()) {
                val bukkit = player.bukkitEntity
                val viewDistanceSquared = bukkit.viewDistance.square() shl 8
                if (entity === player) continue
                val dist = (player.x - entity.x).square() + (player.z - entity.z).square()
                if (dist > viewDistanceSquared) continue
                val cullData = CullDataManager[bukkit]
                if (!cullData.shouldCull) continue
                if (bukkit.checkTickThread()) {
                    cullData.setCulled(event.entity, entity.id, true, pend = false)
                    bukkit.hideEntity(bootstrap, event.entity)
                }
            }
        }

        @EventHandler
        fun onChunkLoad(event: ChunkLoadEvent) {
            for (player in event.world.players) {
                if (!player.checkTickThread()) continue
                val viewDistance = player.viewDistance + 2
                val playerChunk = player.chunk

                if (abs(event.chunk.x - playerChunk.x) > viewDistance || abs(event.chunk.z - playerChunk.z) > viewDistance)
                    continue

                val cullData = CullDataManager[player]
                if (!cullData.shouldCull) continue
                for (entity in event.chunk.entities) {
                    cullData.setCulled(entity, entity.entityId, true, pend = false)
                    @Suppress("DEPRECATION") // Stable API
                    player.hideEntity(bootstrap, entity)
                }
            }
        }

    }

    fun tickPlayer(player: ServerPlayer, bukkit: Player, userCullData: UserCullData, level: ServerLevel, entities: List<SortedEntities>) {
        val viewDistanceSquared = (bukkit.viewDistance + 1).square() shl 8

        val shouldCull = userCullData.shouldCull
        val predicatedPlayerPos = if (shouldCull && config.predicatePlayerPositon) {
            val velocity = playerVelocityGetter.getPlayerMoveVelocity(player)
            if (velocity.lengthSqr() >= 0.06) { // Threshold for sprinting
                var x = player.x
                var y = player.eyeY
                var z = player.z

                var vx = velocity.x
                var vy = velocity.y
                var vz = velocity.z

                for (i in 0 until 3) {
                    x += vx
                    y += vy
                    z += vz

                    vx *= 0.91f
                    vy *= 0.98f
                    vz *= 0.91f
                }
                Vec3(x, y, z)
            } else null
        } else null

        var tickedEntities = 0

        for ((trackRange, entities) in entities) {
            val maxRange = min(trackRange, viewDistanceSquared)
            for (entity in entities) {
                if (entity === player) continue
                val dist = (player.x - entity.x).square() + (player.z - entity.z).square()
                if (dist > maxRange) continue

                tickedEntities++

                if (
                    !shouldCull
                    || entity.isCurrentlyGlowing
                    || config.visibleEntityTypes.contains(entity.type)
                    || dist + (player.y - entity.y).square() <= forceVisibleDistanceSquared
                ) {
                    userCullData.setCulled(entity.bukkitEntity, entity.id, false)
                    continue
                }

                userCullData.setCulled(entity.bukkitEntity, entity.id, raytrace(player, predicatedPlayerPos, entity, level))
            }
        }
        userCullData.shouldCull = tickedEntities >= config.cullThreshold
        userCullData.tick()
    }

    private fun Location.toVec3(): Vec3 {
        return Vec3(x, y, z)
    }

    fun raytrace(player: ServerPlayer, predPlayer: Vec3?, entity: Entity, level: ServerLevel): Boolean {
        val from = player.eyePosition
        val aabb = entity.boundingBox

        val isXMin = abs(from.x - aabb.minX) < abs(from.x - aabb.maxX)
        val isYMin = abs(from.y - aabb.minY) < abs(from.y - aabb.maxY)
        val isZMin = abs(from.z - aabb.minZ) < abs(from.z - aabb.maxZ)

        val nearestX = if (isXMin) aabb.minX else aabb.maxX
        val nearestY = if (isYMin) aabb.minY else aabb.maxY
        val nearestZ = if (isZMin) aabb.minZ else aabb.maxZ
        val farthestX = if (isXMin) aabb.maxX else aabb.minX
        val farthestY = if (isYMin) aabb.maxY else aabb.minY
        val farthestZ = if (isZMin) aabb.maxZ else aabb.minZ

        // Find visible vertices
        // If the player is very close to the entity, then they may only see 1 face(4 vertices) or 2 face (6 vertices)
        // But we don't consider it because it's too rare and raytrace of that should be easy.
        val vertices = listOf(
            Vec3(nearestX, nearestY, nearestZ), Vec3(farthestX, nearestY, nearestZ),
            Vec3(nearestX, farthestY, nearestZ), Vec3(nearestX, nearestY, farthestZ),
            Vec3(farthestX, farthestY, nearestZ), Vec3(farthestX, nearestY, farthestZ),
            Vec3(nearestX, farthestY, farthestZ)
        )

        for (vec3 in vertices) {
            if (!raytracer.raytrace(from, vec3, level)) {
                return false
            }
        }
        if (predPlayer != null) {
            for (vec3 in vertices) {
                if (!raytracer.raytrace(predPlayer, vec3, level)) {
                    return false
                }
            }
        }
        return true
    }

    interface RayTracer {
        fun raytrace(from: Vec3, to: Vec3, level: Level): Boolean
    }

    object StepRayTracer: RayTracer {
        @Suppress("DuplicatedCode")
        override fun raytrace(from: Vec3, to: Vec3, level: Level): Boolean {
            var stepX = to.x - from.x
            var stepY = to.y - from.y
            var stepZ = to.z - from.z

            var x = from.x
            var y = from.y
            var z = from.z

            val length = sqrt(stepX.square() + stepY.square() + stepZ.square())

            stepX /= length
            stepY /= length
            stepZ /= length

            var chunkSections: Array<LevelChunkSection> = INIT_SECTION
            var section: PalettedContainer<BlockState>? = null
            var lastChunkX = Int.MIN_VALUE
            var lastChunkY = Int.MIN_VALUE
            var lastChunkZ = Int.MIN_VALUE
            val minSection = level.dimensionType().minY() shr 4

            for (i in 0 ..< length.toInt()) {
                x += stepX
                y += stepY
                z += stepZ

                val currX = x.floorI()
                val currY = y.floorI()
                val currZ = z.floorI()

                val newChunkX = currX shr 4
                val newChunkY = currY shr 4
                val newChunkZ = currZ shr 4

                val chunkDiff = (newChunkX xor lastChunkX) or (newChunkZ xor lastChunkZ)
                val sectionDiff = newChunkY xor lastChunkY

                if (chunkDiff or sectionDiff != 0) {
                    if (chunkDiff != 0) {
                        // If chunk is not loaded, consider blocked (Player should not see the entity either!)
                        val chunk = level.getChunkIfLoaded(newChunkX, newChunkZ) ?: return true
                        chunkSections = chunk.sections
                    }
                    val sectionIndex = newChunkY - minSection
                    if (sectionIndex !in (0 until chunkSections.size)) continue
                    section = chunkSections[sectionIndex].states

                    lastChunkX = newChunkX
                    lastChunkY = newChunkY
                    lastChunkZ = newChunkZ
                }

                if (section != null) { // It can never be null, but we don't want the kotlin npe check!
                    val blockState = section.get((currX and 15) or ((currZ and 15) shl 4) or ((currY and 15) shl (4 + 4)))
                    if (!shapedOcclusion[blockState] && canOcclude[blockState])
                        return true
                }
            }
            return false
        }
    }

    object DDARayTracer: RayTracer {
        @Suppress("DuplicatedCode")
        override fun raytrace(from: Vec3, to: Vec3, level: Level): Boolean {
            val adjX = COLLISION_EPSILON * (from.x - to.x)
            val adjY = COLLISION_EPSILON * (from.y - to.y)
            val adjZ = COLLISION_EPSILON * (from.z - to.z)

            if (adjX == 0.0 && adjY == 0.0 && adjZ == 0.0) {
                return false
            }

            val toXAdj = to.x - adjX
            val toYAdj = to.y - adjY
            val toZAdj = to.z - adjZ
            val fromXAdj = from.x + adjX
            val fromYAdj = from.y + adjY
            val fromZAdj = from.z + adjZ

            var currX = fromXAdj.floorI()
            var currY = fromYAdj.floorI()
            var currZ = fromZAdj.floorI()

            val diffX = toXAdj - fromXAdj
            val diffY = toYAdj - fromYAdj
            val diffZ = toZAdj - fromZAdj

            val dxDouble = sign(diffX)
            val dyDouble = sign(diffY)
            val dzDouble = sign(diffZ)

            val dx = dxDouble.toInt()
            val dy = dyDouble.toInt()
            val dz = dzDouble.toInt()

            val normalizedDiffX = if (diffX == 0.0) Double.MAX_VALUE else dxDouble / diffX
            val normalizedDiffY = if (diffY == 0.0) Double.MAX_VALUE else dyDouble / diffY
            val normalizedDiffZ = if (diffZ == 0.0) Double.MAX_VALUE else dzDouble / diffZ

            var normalizedCurrX = normalizedDiffX * (if (diffX > 0.0) (1.0 - fromXAdj.frac()) else fromXAdj.frac())
            var normalizedCurrY = normalizedDiffY * (if (diffY > 0.0) (1.0 - fromYAdj.frac()) else fromYAdj.frac())
            var normalizedCurrZ = normalizedDiffZ * (if (diffZ > 0.0) (1.0 - fromZAdj.frac()) else fromZAdj.frac())

            var chunkSections: Array<LevelChunkSection> = INIT_SECTION
            var section: PalettedContainer<BlockState>? = null
            var lastChunkX = Int.MIN_VALUE
            var lastChunkY = Int.MIN_VALUE
            var lastChunkZ = Int.MIN_VALUE

            val minSection = level.dimensionType().minY() shr 4

            while (true) {
                val newChunkX = currX shr 4
                val newChunkY = currY shr 4
                val newChunkZ = currZ shr 4

                val chunkDiff = ((newChunkX xor lastChunkX) or (newChunkZ xor lastChunkZ))
                val chunkYDiff = newChunkY xor lastChunkY

                if ((chunkDiff or chunkYDiff) != 0) {
                    if (chunkDiff != 0) {
                        val chunk = level.getChunkIfLoaded(newChunkX, newChunkZ) ?: return true
                        chunkSections = chunk.sections
                    }
                    val sectionIndex = newChunkY - minSection
                    section = if (sectionIndex in (0 until chunkSections.size)) chunkSections[sectionIndex].states else null

                    lastChunkX = newChunkX
                    lastChunkY = newChunkY
                    lastChunkZ = newChunkZ
                }

                if (section != null) {
                    val blockState = section.get((currX and 15) or ((currZ and 15) shl 4) or ((currY and 15) shl (4 + 4)))
                    if (!shapedOcclusion[blockState] && canOcclude[blockState])
                        return true
                }

                if (normalizedCurrX > 1.0 && normalizedCurrY > 1.0 && normalizedCurrZ > 1.0) {
                    return false
                }

                if (normalizedCurrX < normalizedCurrY) {
                    if (normalizedCurrX < normalizedCurrZ) {
                        currX += dx
                        normalizedCurrX += normalizedDiffX
                    } else {
                        currZ += dz
                        normalizedCurrZ += normalizedDiffZ
                    }
                } else if (normalizedCurrY < normalizedCurrZ) {
                    currY += dy
                    normalizedCurrY += normalizedDiffY
                } else {
                    currZ += dz
                    normalizedCurrZ += normalizedDiffZ
                }
            }
        }
    }

    object Commands {

        private const val BENCHMARK_DATA_SIZE = 100_000

        @Command("esu networkThrottle entityCulling benchmark")
        @ShortPerm
        fun benchmark(sender: User, @Flag("singleThread") singleThread: Boolean = false) {
            val dataset = prepareBenchmark(sender as PlayerUser)
            sender.message("Running benchmark (${if (singleThread) "singleThread" else "multiThreads"})")
            runBlocking {
                val coroutine = coroutine!!
                val count = AtomicInteger()
                val threads = if (singleThread) 1 else lastThreads
                val jobs = buildList(threads) {
                    repeat(threads) {
                        val job = launch(coroutine) {
                            var i = 0
                            while (isActive) {
                                raytracer.raytrace(dataset.from, dataset.data[i++], dataset.level)
                                if (i == BENCHMARK_DATA_SIZE) i = 0
                                count.incrementAndGet()
                            }
                        }
                        add(job)
                    }
                }
                delay(1.seconds)
                jobs.forEach { it.cancel() }
                sender.message("Raytrace $count times in 1 seconds")
                sender.message("Max of ${count.get() / 7 / 20} entities per game tick")
                sender.message("Test result is for reference only.")
            }
        }

        @Command("esu networkThrottle entityCulling stats")
        @ShortPerm
        fun stats(sender: User) {
            sender.message("Previous elapsedTime: ${previousElapsedTime}ms ; delayTime: ${previousDelayTime}ms")
        }

        private fun prepareBenchmark(user: PlayerUser): BenchmarkDataset {
            val player = user.player
            user.message("Preparing data at this spot...")
            val from = player.eyeLocation.toVec3()
            val world = player.world
            val viewDistance = world.viewDistance - 2
            val level = (world as CraftWorld).handle
            val data = Array(BENCHMARK_DATA_SIZE) {
                from.add(
                    (-16 * viewDistance .. 16 * viewDistance).random().toDouble(),
                    (world.minHeight .. floor(from.y).toInt() + 48).random().toDouble(),
                    (-16 * viewDistance .. 16 * viewDistance).random().toDouble(),
                )
            }
            return BenchmarkDataset(level, from, data)
        }

        private class BenchmarkDataset(
            val level: ServerLevel,
            val from: Vec3,
            val data: Array<Vec3>,
        )

        private fun Location.toVec3(): Vec3 {
            return Vec3(x, y, z)
        }
    }

    data class SortedEntities(val trackRangeSquared: Int, val entities: List<Entity>)

    data class RaytraceConfig(
        @Comment("Asynchronous threads used to calculate visibility. More to update faster.")
        val raytraceThreads: Int = Runtime.getRuntime().availableProcessors() / 3,
        @Comment("""
            Max updates for each player per second.
            More means greater immediacy, but also higher cpu usage.
        """)
        val updatesPerSecond: Int = 15,
        @Comment("""
            Enabling fast-raytrace uses fixed-distance steps, which calculates nearly 100% faster, but
             entities cross corners may not hidden, also preventing entities from suddenly appearing.
            Set to false to use 3D-DDA algorithm, for scenarios requiring more accurate results.
        """)
        val fastRaytrace: Boolean = true,
        @Comment("""
            Mark entities culled at first seen.
            To prevent flickering on entity spawning, and related packets.
        """)
        val entityCulledByDefault: Boolean = true,
        @Comment("These entity types are considered always visible.")
        val visibleEntityTypes: Set<EntityType<*>> = setOf(EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.PLAYER),
        @Comment("Entities within this radius are considered always visible.")
        val forceVisibleDistance: Double = 8.0,
        @Comment("""
            Simulate and predicate player positon behind later game ticks.
            An entity will only be culled if it is not visible at either
             the player's current positon or the predicted positon.
            This can reduce the possibility of entity flickering.
            May double the raytrace time depends on the player velocity.
            Requires Minecraft 1.21+ for client movement velocity.
        """)
        val predicatePlayerPositon: Boolean = true,
        @Comment("""
            Player must can see this amount of entities before we start culling for them.
            Set -1 to always do culling.
        """)
        val cullThreshold: Int = -1,
    ): ConfigurationPart

}