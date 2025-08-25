package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Waterlogged
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import kotlin.jvm.java

object OptimizationsModule: BukkitModule<OptimizationsModule.ModuleConfig, EmptyConfiguration>(
    ModuleConfig::class.java, EmptyConfiguration::class.java
) {

    override fun enable() {
        registerListener(object : Listener {
            @EventHandler
            fun onLiquidSpread(e: BlockFromToEvent) {
                if (e.block.blockData is Waterlogged) {
                    val config = config.waterlogged
                    if (config.disableWaterSpread) {
                        e.isCancelled = true
                    }
                }
            }
            @EventHandler
            fun onPistonPushWaterlogged(e: BlockPistonExtendEvent) {
                handleWaterloggedPush(e.blocks, e.direction)
            }
            @EventHandler
            fun onPistonPushWaterlogged(e: BlockPistonRetractEvent) {
                handleWaterloggedPush(e.blocks, e.direction)
            }

            private fun handleWaterloggedPush(blocks: List<Block>, face: BlockFace) {
                if (!config.waterlogged.disableWaterSpread || !config.waterlogged.keepWaterAfterPistonPush) {
                    return
                }
                for (block in blocks) {
                    val waterlogged = block.blockData as? Waterlogged ?: continue
                    if (waterlogged.isWaterlogged)
                        Scheduler.schedule(block.location, 3) {
                            val moved = block.getRelative(face)
                            val blockData = moved.blockData as Waterlogged
                            blockData.isWaterlogged = true
                            moved.blockData = blockData
                        }
                }
            }
        })
    }


    data class ModuleConfig(
        val waterlogged: Waterlogged = Waterlogged(),
    ): BaseModuleConfiguration() {

        data class Waterlogged(
            @Comment("Enable this will disable water spread from Waterlogged blocks.")
            val disableWaterSpread: Boolean = false,
            @Comment("If enabled, water in Waterlogged will be refilled after a piston push.")
            val keepWaterAfterPistonPush: Boolean = false,
        )
    }

}