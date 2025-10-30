package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.v1

import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.RaytraceHandler
import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.UserCullData
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelEntitiesHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelHandler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
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
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.util.NumberConversions
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

class RaytraceHandlerImpl: RaytraceHandler<RaytraceHandlerImpl.RaytraceConfig, EmptyConfiguration>() {

    companion object {
        private const val COLLISION_EPSILON = 1E-7
    }

    private var forceVisibleDistanceSquared = 0.0

    override fun onReload() {
        super.onReload()
        forceVisibleDistanceSquared = config.forceVisibleDistance * config.forceVisibleDistance
    }

    data class RaytraceConfig(
        @Comment("These entity types are considered always visible.")
        val visibleEntityTypes: Set<EntityType<*>> = setOf(EntityType.WITHER, EntityType.ENDER_DRAGON),
        @Comment("Entities within this radius are considered always visible.")
        val forceVisibleDistance: Double = 8.0,
    ): ConfigurationPart

    val levelGetter by Versioned(LevelHandler::class.java)
    val levelEntitiesHandler by Versioned(LevelEntitiesHandler::class.java)

    override fun tickPlayer(bukkitPlayer: Player, userCullData: UserCullData) {
        val viewDistanceSquared = bukkitPlayer.viewDistance.let { it * it } shl 8
        val player = (bukkitPlayer as CraftPlayer).handle
        val level = levelGetter.level(player)
        for (entity in levelEntitiesHandler.getEntitiesAll(level)) {
//                            entity.bukkitEntity.getNearbyEntities()
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

            userCullData.setCulled(entity.bukkitEntity, entity.id, raytrace(player, entity, level))
        }
        userCullData.tick()
    }

    override fun raytrace(from: Vector, to: Vector, world: World): Boolean {
        return raytraceStep(from.toVec3(), to.toVec3(), (world as CraftWorld).handle)
    }

    override fun getEntityId(entity: org.bukkit.entity.Entity): Int {
        return (entity as CraftEntity).handle.id
    }

    private fun Vector.toVec3(): Vec3 {
        return Vec3(x, y, z)
    }

    fun raytrace(player: ServerPlayer, entity: Entity, level: ServerLevel): Boolean {
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