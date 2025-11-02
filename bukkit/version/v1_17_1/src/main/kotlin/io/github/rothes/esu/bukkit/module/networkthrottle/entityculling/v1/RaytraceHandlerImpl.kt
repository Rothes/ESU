package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.v1

import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.CullDataManager
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerVelocityGetter
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.RaytraceHandler
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.UserCullData
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelEntitiesHandler
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.meta.RenamedFrom
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import kotlinx.coroutines.*
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity
import org.bukkit.entity.Player
import org.bukkit.util.NumberConversions
import org.bukkit.util.Vector
import org.incendo.cloud.annotations.Command
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

class RaytraceHandlerImpl: RaytraceHandler<RaytraceHandlerImpl.RaytraceConfig, EmptyConfiguration>() {

    companion object {
        private const val COLLISION_EPSILON = 1E-7
    }

    private var forceVisibleDistanceSquared = 0.0
    private var millisBetweenUpdates = 50

    private var lastThreads = 0
    private var coroutine: ExecutorCoroutineDispatcher? = null

    private var previousElapsedTime = 0L
    private var previousDelayTime = 0L

    override fun checkConfig(): Feature.AvailableCheck? {
        if (config.raytraceThreads < 1) {
            plugin.err("[EntityCulling] At least one raytrace thread is required to enable this feature.")
            return Feature.AvailableCheck.fail { "At least one raytrace thread is required!".message }
        }
        return null
    }

    override fun onReload() {
        super.onReload()
        forceVisibleDistanceSquared = config.forceVisibleDistance * config.forceVisibleDistance
        millisBetweenUpdates = 1000 / config.updatesPerSecond
        if (enabled) {
            if (lastThreads != config.raytraceThreads) {
                start()
            }
        }
    }

    override fun onEnable() {
        start()
        registerCommands(object {
            @Command("esu networkThrottle entityCulling benchmark")
            @ShortPerm
            fun benchmark(sender: User) {
                val user = sender as PlayerUser
                val player = user.player
                sender.message("Preparing data at this spot...")
                val loc = player.eyeLocation
                val world = loc.world
                val maxI = 100_000_00
                val viewDistance = world.viewDistance - 2
                val data = Array(maxI) {
                    loc.clone().add(
                        (-16 * viewDistance .. 16 * viewDistance).random().toDouble(),
                        (world.minHeight .. loc.blockY + 48).random().toDouble(),
                        (-16 * viewDistance .. 16 * viewDistance).random().toDouble(),
                    ).toVector()
                }
                val from = loc.toVector()
                var i = 0
                sender.message("Running benchmark")
                runBlocking {
                    var count = 0
                    val jobs = buildList(4) {
                        repeat(4) {
                            val job = launch(coroutine!!) {
                                while (isActive) {
                                    var get = ++i
                                    if (i >= maxI) {
                                        i = 0
                                        get = 0
                                    }
                                    raytrace(from, data[get], world)
                                    count++
                                }
                            }
                            add(job)
                        }
                    }
                    delay(1.seconds)
                    jobs.forEach { it.cancel() }
                    sender.message("Raytrace $count times in 1 seconds")
                    sender.message("Max of ${count / 7 / 20} entities per tick")
                    sender.message("Test result is for reference only.")
                }
            }
        })
    }

    override fun onDisable() {
        super.onDisable()
        coroutine?.close()
        coroutine = null
        lastThreads = 0
    }

    private fun start() {
        coroutine?.close()
        val nThreads = config.raytraceThreads

        val name = "ESU-EntityCulling"
        val threadNo = AtomicInteger()
        val executor = Executors.newScheduledThreadPool(nThreads) { runnable ->
            Thread(runnable, if (nThreads == 1) name else name + "-" + threadNo.incrementAndGet())
                .apply {
                    priority = Thread.NORM_PRIORITY - 1
                    isDaemon = true
                }
        }
        val context = Executors.unconfigurableExecutorService(executor).asCoroutineDispatcher()
        CoroutineScope(context).launch {
            while (isActive) {
                val millis = System.currentTimeMillis()
                Bukkit.getWorlds().flatMapTo(
                    ArrayList(Bukkit.getOnlinePlayers().size + 1)
                ) { bukkitWorld ->
                    val level = (bukkitWorld as CraftWorld).handle
                    val players = level.players()
                    if (players.isEmpty()) return@flatMapTo emptyList()
                    val entities = levelEntitiesHandler.getEntitiesAll(level)
                    players.map { player ->
                        launch {
                            val bp = player.bukkitEntity
                            try {
                                tickPlayer(player, bp,  CullDataManager[bp], level, entities)
                            } catch (e: Throwable) {
                                plugin.err("[EntityCulling] Failed to update player ${bp.name}", e)
                            }
                        }
                    }
                }.joinAll()
                val elapsed = System.currentTimeMillis() - millis
                val delay = millisBetweenUpdates - elapsed
                previousElapsedTime = elapsed
                previousDelayTime = delay
                delay(delay)
            }
        }
        lastThreads = nThreads
        coroutine = context
    }

    data class RaytraceConfig(
        @Comment("Asynchronous threads used to calculate visibility. More to update faster.")
        @RenamedFrom("./raytrace-threads")
        val raytraceThreads: Int = Runtime.getRuntime().availableProcessors() / 3,
        @Comment("""
            Max updates for each player per second.
            More means greater immediacy, but also higher cpu usage.
        """)
        @RenamedFrom("./updates-per-second")
        val updatesPerSecond: Int = 15,
        @Comment("These entity types are considered always visible.")
        val visibleEntityTypes: Set<EntityType<*>> = setOf(EntityType.WITHER, EntityType.ENDER_DRAGON),
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
    ): ConfigurationPart

    val levelEntitiesHandler by Versioned(LevelEntitiesHandler::class.java)
    val playerVelocityGetter by Versioned(PlayerVelocityGetter::class.java)

    fun tickPlayer(player: ServerPlayer, bukkit: Player, userCullData: UserCullData, level: ServerLevel, entities: Iterable<Entity>) {
        val viewDistanceSquared = bukkit.viewDistance.let { it * it } shl 8

        val predicatedPlayerPos = if (config.predicatePlayerPositon) {
            val velocity = playerVelocityGetter.getPlayerMoveVelocity(player)
            if (velocity.lengthSqr() >= 0.06) { // Threshold for sprinting
                var x = player.x
                var y = player.eyeY
                var z = player.z

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

        // `level.entityLookup.all` + distance check is already the fastest way to collect all entities to check.
        // Get regions from entityLookup, then loop over each chunk to collect entities is 2x slower.
        for (entity in entities) {
            if (entity == player) continue
            val x = player.x - entity.x
            val z = player.z - entity.z
            val dist = x * x + z * z
            if (dist > viewDistanceSquared) continue

            val y = player.y - entity.y
            if (entity.isCurrentlyGlowing || config.visibleEntityTypes.contains(entity.type) || dist + y * y <= forceVisibleDistanceSquared) {
                userCullData.setCulled(entity.bukkitEntity, entity.id, false)
                continue
            }

            userCullData.setCulled(entity.bukkitEntity, entity.id, raytrace(player, predicatedPlayerPos, entity, level))
        }
        userCullData.tick()
    }

    fun raytrace(from: Vector, to: Vector, world: World): Boolean {
        return raytraceStep(from.toVec3(), to.toVec3(), (world as CraftWorld).handle)
    }

    override fun getEntityId(entity: org.bukkit.entity.Entity): Int {
        return (entity as CraftEntity).handle.id
    }

    private fun Vector.toVec3(): Vec3 {
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
            Vec3(nearestX, nearestY, nearestZ),
            Vec3(farthestX, nearestY, nearestZ),
            Vec3(nearestX, farthestY, nearestZ),
            Vec3(nearestX, nearestY, farthestZ),
            Vec3(farthestX, farthestY, nearestZ),
            Vec3(farthestX, nearestY, farthestZ),
            Vec3(nearestX, farthestY, farthestZ)
        )

        for (vec3 in vertices) {
            if (!raytraceStep(from, vec3, level)) {
                return false
            }
        }
        if (predPlayer != null) {
            for (vec3 in vertices) {
                if (!raytraceStep(predPlayer, vec3, level)) {
                    return false
                }
            }
        }
        return true
    }

    fun raytraceStep(from: Vec3, to: Vec3, level: Level): Boolean {
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

        var chunkSections: Array<LevelChunkSection> = arrayOf()
        var section: PalettedContainer<BlockState>? = null
        var lastChunkX = Int.MIN_VALUE
        var lastChunkY = Int.MIN_VALUE
        var lastChunkZ = Int.MIN_VALUE
        val minSection = level.dimensionType().minY() shr 4

        for (i in 0 ..< length.toInt()) {
            x += stepX
            y += stepY
            z += stepZ

            val currX = NumberConversions.floor(x)
            val currY = NumberConversions.floor(y)
            val currZ = NumberConversions.floor(z)

            val newChunkX = currX shr 4
            val newChunkY = currY shr 4
            val newChunkZ = currZ shr 4

            val chunkDiff = (newChunkX xor lastChunkX) or (newChunkZ xor lastChunkZ)
            val sectionDiff = newChunkY xor lastChunkY

            if (chunkDiff or sectionDiff != 0) {
                if (chunkDiff != 0) {
                    chunkSections = level.getChunk(newChunkX, newChunkZ).getSections()
                }
                val sectionIndex = newChunkY - minSection
                if (sectionIndex !in (0 until chunkSections.size)) continue
                section = chunkSections[sectionIndex].states

                lastChunkX = newChunkX
                lastChunkY = newChunkY
                lastChunkZ = newChunkZ
            }

            if (section != null) { // It can never be null, but we don't want the kotlin npc check!
                val blockState = section.get((currX and 15) or ((currZ and 15) shl 4) or ((currY and 15) shl (4 + 4)))
//                blockState.`moonrise$getTableIndex`()
                if (!blockState.isAir && blockState.bukkitMaterial.isOccluding) {
                    return true
                }
            }
        }
        return false
    }

    fun raytraceDDA(from: Location, to: Location, level: Level): Boolean {
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

        var currX = Mth.floor(fromXAdj)
        var currY = Mth.floor(fromYAdj)
        var currZ = Mth.floor(fromZAdj)

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

        var normalizedCurrX = normalizedDiffX * (if (diffX > 0.0) (1.0 - Mth.frac(fromXAdj)) else Mth.frac(fromXAdj))
        var normalizedCurrY = normalizedDiffY * (if (diffY > 0.0) (1.0 - Mth.frac(fromYAdj)) else Mth.frac(fromYAdj))
        var normalizedCurrZ = normalizedDiffZ * (if (diffZ > 0.0) (1.0 - Mth.frac(fromZAdj)) else Mth.frac(fromZAdj))

        var lastChunk: Array<LevelChunkSection>? = null
        var lastSection: PalettedContainer<BlockState>? = null
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
                    lastChunk = level.getChunk(newChunkX, newChunkZ).getSections()
                }
                val sectionY = newChunkY - minSection
                lastSection = if (sectionY >= 0 && sectionY < lastChunk!!.size) lastChunk[sectionY].states else null

                lastChunkX = newChunkX
                lastChunkY = newChunkY
                lastChunkZ = newChunkZ
            }

            if (lastSection != null) {
                val blockState = lastSection.get((currX and 15) or ((currZ and 15) shl 4) or ((currY and 15) shl (4 + 4)))
                if (!blockState.isAir && blockState.bukkitMaterial.isOccluding) {
                    return true
                }
            }

            if (normalizedCurrX > 1.0 && normalizedCurrY > 1.0 && normalizedCurrZ > 1.0) {
                return false
            }

            // inc the smallest normalized coordinate
            if (normalizedCurrX < normalizedCurrY) {
                if (normalizedCurrX < normalizedCurrZ) {
                    currX += dx
                    normalizedCurrX += normalizedDiffX
                } else {
                    // x < y && x >= z <--> z < y && z <= x
                    currZ += dz
                    normalizedCurrZ += normalizedDiffZ
                }
            } else if (normalizedCurrY < normalizedCurrZ) {
                // y <= x && y < z
                currY += dy
                normalizedCurrY += normalizedDiffY
            } else {
                // y <= x && z <= y <--> z <= y && z <= x
                currZ += dz
                normalizedCurrZ += normalizedDiffZ
            }
        }
    }

    private fun Double.square() = this * this

}