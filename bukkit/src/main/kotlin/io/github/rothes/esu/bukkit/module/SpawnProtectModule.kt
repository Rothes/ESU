package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Entity
import org.bukkit.entity.Wither
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.world.ChunkLoadEvent
import kotlin.math.abs
import kotlin.math.max

object SpawnProtectModule: BukkitModule<SpawnProtectModule.ModuleConfig, EmptyConfiguration>() {

    override fun onEnable() {
        update()
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        if (plugin.isEnabled) {
            // This can only run when enabled
            for (world in Bukkit.getWorlds()) {
                for (chunk in world.loadedChunks) {
                    Scheduler.schedule(chunk.getBlock(0, 0, 0).location) {
                        for (entity in chunk.entities) {
                            resetEntity(entity)
                        }
                    }
                }
            }
        }
        Listeners.unregister()
    }

    override fun onReload() {
        super.onReload()
        if (enabled) {
            update()
        }
    }

    fun update() {
        for (world in Bukkit.getWorlds()) {
            for (chunk in world.loadedChunks) {
                Scheduler.schedule(chunk.getBlock(0, 0, 0).location) {
                    for (entity in chunk.entities) {
                        handleEntity(entity)
                    }
                }
            }
        }
    }

    private val witherNerfKey = NamespacedKey.fromString("wither-mod", bootstrap)!!
    fun handleEntity(e: Entity) {
        if (e !is Wither) return

        resetEntity(e)
        val nerf = config.spawnWitherNerf.firstOrNull { it.radius >= max(abs(e.location.x), abs(e.location.z)) } ?: return

        e.getAttribute(AttributeAdapter.FLYING_SPEED)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey, -1 + nerf.speedModifier, AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
        e.getAttribute(AttributeAdapter.MOVEMENT_SPEED)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey,
                -1 + nerf.speedModifier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
        e.getAttribute(AttributeAdapter.FOLLOW_RANGE)!!.addTransientModifier(
            AttributeModifier(
                witherNerfKey,
                -1 + nerf.followRangeModifier,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1
            )
        )
    }

    fun resetEntity(e: Entity) {
        if (e !is Wither) return

        e.getAttribute(AttributeAdapter.FLYING_SPEED)?.removeModifier(witherNerfKey)
        e.getAttribute(AttributeAdapter.MOVEMENT_SPEED)?.removeModifier(witherNerfKey)
        e.getAttribute(AttributeAdapter.FOLLOW_RANGE)?.removeModifier(witherNerfKey)
    }

    object Listeners: Listener {

        @EventHandler
        fun onEntitySpawn(e: EntitySpawnEvent) {
            if (e.entity !is Wither)
                return
            val nerf = config.spawnWitherNerf.firstOrNull { it.radius >= max(abs(e.location.x), abs(e.location.z)) } ?: return

            val amount = e.entity.chunk.entities.filter { it is Wither }.size
            if (amount >= nerf.maxAmount) {
                e.isCancelled = true
                return
            }
            handleEntity(e.entity)
        }

        @EventHandler
        fun onChunkLoad(e: ChunkLoadEvent) {
            val nerf = config.spawnWitherNerf.firstOrNull { it.radius shr 4 >= max(abs(e.chunk.x), abs(e.chunk.z)) } ?: return

            val withers = e.chunk.entities.filterIsInstance<Wither>()
            val max = nerf.maxAmount
            val amount = withers.size
            if (amount > max) {
                for (entity in withers.takeLast(amount - max)) entity.remove()
                for (entity in withers.dropLast(amount - max)) handleEntity(entity)
                return
            }
            for (entity in withers) handleEntity(entity)
        }

        @EventHandler
        fun onPistonPushWaterlogged(e: BlockPistonExtendEvent) {
            handleWaterloggedPush(e.blocks, e)
        }
        @EventHandler
        fun onPistonPushWaterlogged(e: BlockPistonRetractEvent) {
            handleWaterloggedPush(e.blocks, e)
        }

        private fun handleWaterloggedPush(blocks: MutableList<Block>, e: BlockPistonEvent) {
            val config = config.waterlogged
            if (config.disableSpawnPushRadius > 0) {
                val dist = abs(e.block.x) + abs(e.block.z)
                if (dist <= config.disableSpawnPushRadius
                    && blocks.any { (it.blockData as? Waterlogged)?.isWaterlogged == true }) {
                    e.isCancelled = true
                }
            }
        }
    }

    data class ModuleConfig(
        val spawnWitherNerf: List<SpawnWitherNerf> = listOf(SpawnWitherNerf(256, 0), SpawnWitherNerf()),
        val waterlogged: Waterlogged = Waterlogged(),
    ): BaseModuleConfiguration() {

        data class SpawnWitherNerf(
            val radius: Long = 1024,
            val maxAmount: Int = 1,
            val speedModifier: Double = 0.5,
            val followRangeModifier: Double = 0.25,
        )

        data class Waterlogged(
            @Comment("""
                Disable pushing waterlogged blocks in spawn circle range.
                set to -1 to disable the limit.
                This is to prevent ocean maker machine.
            """)
            val disableSpawnPushRadius: Long = -1
        )
    }

}