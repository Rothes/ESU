package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.v18

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerVelocityGetter
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.RaytraceHandler
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.entity.PlayerEntityVisibilityProcessor
import io.github.rothes.esu.bukkit.util.extension.createChild
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.*
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.extension.math.floorI
import io.github.rothes.esu.core.util.extension.math.frac
import io.github.rothes.esu.core.util.extension.math.square
import it.unimi.dsi.fastutil.objects.ReferenceSet
import kotlinx.coroutines.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

object RaytraceHandlerImpl: RaytraceHandler<RaytraceHandlerImpl.RaytraceConfig, EmptyConfiguration>() {

    private const val COLLISION_EPSILON = 1E-7
    private val INIT_SECTION: Array<LevelChunkSection> = arrayOf()
    private val ENTITY_TYPES: Int

    init {
        val registryAccessHandler = NmsRegistryAccessHandler::class.java.versioned()
        val registries = NmsRegistries::class.java.versioned()
        val registry = registryAccessHandler.getRegistryOrThrow(registries.entityType)
        ENTITY_TYPES = registryAccessHandler.size(registry)
    }

    private val pl = plugin.createChild(name = "${plugin.name}-EntityCulling")
    private val players = ConcurrentHashMap<Player, VisibilityProcessor>()

    private val VELOCITY_GETTER by Versioned(PlayerVelocityGetter::class.java)
    private val LEVEL_GETTER by Versioned(LevelHandler::class.java)
    private val HANDLE_GETTER by Versioned(EntityHandleGetter::class.java)
    private val OCCLUDE_TESTER by Versioned(BlockOccludeTester::class.java)

    private var raytracer: RayTracer = StepRayTracer
    private var forceVisibleDistanceSquared = 0.0
    private var millisBetweenUpdates = 50

    private var lastThreads = 0
    private var coroutine: ExecutorCoroutineDispatcher? = null

    private var previousElapsedTime = 0L
    private var previousDelayTime = 0L

    override fun checkConfig(): Feature.AvailableCheck? {
        if (config.raytraceThreads < 1) {
            core.err("[EntityCulling] At least one raytrace thread is required to enable this feature.")
            return Feature.AvailableCheck.fail { "At least one raytrace thread is required!".message }
        }
        return null
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
        for (player in Bukkit.getOnlinePlayers()) {
            players[player] = VisibilityProcessor(player).also { it.start() }
        }
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
        coroutine?.close()
        coroutine = null
        lastThreads = 0
        for (processor in players.values) {
            processor.shutdown()
        }
        players.clear()
    }

    private fun init() {
        val config = config
        if (lastThreads != config.raytraceThreads)
            startThread()
        raytracer = if (config.fastRaytrace) StepRayTracer else DDARayTracer
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
                players.values.map { processor ->
                    launch {
                        processor.tick()
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

    fun raytrace(player: ServerPlayer, predPlayer: Vec3?, aabb: AABB, level: ServerLevel): Boolean {
        val from = player.eyePosition

        val vertices = if (aabb.xsize <= 0.25 && aabb.ysize <= 0.25 && aabb.zsize <= 0.25) {
            // If it's small boundingBox, only consider center pos.
            // Mostly item, projectile entities.
            listOf(aabb.center)
        } else {
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
            listOf(
                Vec3(nearestX, nearestY, nearestZ),
                Vec3(farthestX, nearestY, nearestZ),
                Vec3(nearestX, farthestY, nearestZ),
                Vec3(nearestX, nearestY, farthestZ),
                Vec3(farthestX, farthestY, nearestZ),
                Vec3(farthestX, nearestY, farthestZ),
                Vec3(nearestX, farthestY, farthestZ)
            )
        }

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

    private object Listeners: Listener {
        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            players[event.player] = VisibilityProcessor(event.player).also { it.start() }
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            players.remove(event.player)?.shutdown()
        }
    }

    class VisibilityProcessor(player: Player): PlayerEntityVisibilityProcessor.OffTick(player, pl) {

        private val serverPlayer = HANDLE_GETTER.getHandle(player) as ServerPlayer
        private var level: ServerLevel = LEVEL_GETTER.level(serverPlayer)
        private var shouldCull = true
        private var predicatedPlayerPos: Vec3? = null

        private var tickedEntities = 0

        override fun shouldHideDefault(entity: org.bukkit.entity.Entity): Boolean {
            // Do not hide Firework, it blocks players from using elytra
            return shouldCull && entity !is Firework && !config.visibleEntityTypes.contains(HANDLE_GETTER.getHandle(entity).type)
        }

        override fun setupUpdate() {
            tickedEntities = 0
            level = LEVEL_GETTER.level(serverPlayer)
            predicatedPlayerPos = if (shouldCull && config.predicatePlayerPositon) {
                val velocity = VELOCITY_GETTER.getPlayerMoveVelocity(serverPlayer)
                if (velocity.lengthSqr() >= 0.06) { // Threshold for sprinting
                    var x = serverPlayer.x
                    var y = serverPlayer.eyeY
                    var z = serverPlayer.z

                    var vx = velocity.x
                    var vy = velocity.y
                    var vz = velocity.z

                    for (i in 0 until 2) {
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
        }

        override fun postUpdate() {
            super.postUpdate()
            shouldCull = tickedEntities >= config.cullThreshold
        }

        override fun shouldHide(entity: Entity, distSqr: Double): HideState {
            tickedEntities++
            if (!shouldCull
                || distSqr <= forceVisibleDistanceSquared
                || entity.isCurrentlyGlowing
                || config.visibleEntityTypes.contains(entity.type)
            ) {
                return HideState.SHOW
            }

            return if (raytrace(serverPlayer, predicatedPlayerPos, entity.boundingBox, level))
                HideState.HIDE else HideState.SHOW
        }

        override fun shouldHide(entity: org.bukkit.entity.Entity, distSqr: Double): HideState {
            error("Use RaytraceHandlerImpl.VisibilityProcessor.shouldHide(net.minecraft.world.entity.Entity, double)")
        }

        override fun scheduleTick() {}

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
                    if (OCCLUDE_TESTER.isFullOcclude(blockState))
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
                    if (OCCLUDE_TESTER.isFullOcclude(blockState))
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
        @Comment("These entity types are considered always visible.")
        val visibleEntityTypes: ReferenceSet<EntityType<*>> = ReferenceSet.of(EntityType.WITHER),
        @Comment("Entities within this radius are considered always visible.")
        val forceVisibleDistance: Double = 8.0,
        @Comment("""
            Simulate and predicate player positon behind later game ticks.
            An entity will only be culled if it is not visible at either
             the player's current positon or the predicted positon.
            This can reduce the possibility of entity suddenly appearing.
            May double the raytrace time depends on the player velocity.
            Requires Minecraft 1.21+ for client movement velocity.
        """)
        val predicatePlayerPositon: Boolean = true,
        @Comment("""
            Player must can see this amount of entities before we start culling for them.
            Set -1 to always do culling.
        """)
        val cullThreshold: Int = -1,
    ): ConfigurationPart {

        @RemovedNode
        val entityCulledByDefault: Boolean = true
    }

}