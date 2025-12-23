package io.github.rothes.esu.bukkit.module.optimizations.antilag

import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent

object WaterloggedFeature: CommonFeature<WaterloggedFeature.FeatureConfig, Unit>() {

    override fun onEnable() {
        Listeners.register()
    }

    override fun onDisable() {
        Listeners.unregister()
    }

    private object Listeners: Listener {
        @EventHandler
        fun onLiquidSpread(e: BlockFromToEvent) {
            if (e.block.blockData is Waterlogged) {
                if (config.disableWaterSpread) {
                    e.isCancelled = true
                }
            }
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
            val config = config
            if (config.disableWaterloggedBlockPush) {
                if (blocks.any { (it.blockData as? Waterlogged)?.isWaterlogged == true }) {
                    e.isCancelled = true
                    return
                }
            }
            if (!config.disableWaterSpread || !config.keepWaterAfterPistonPush) {
                return
            }
            val face = e.direction
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
    }

    data class FeatureConfig(
        @Comment("Enable this will disable water spread from waterlogged blocks.")
        val disableWaterSpread: Boolean = false,
        @Comment("If enabled, water in waterlogged blocks will always be refilled after a piston push.")
        val keepWaterAfterPistonPush: Boolean = false,
        @Comment("""
                If enabled, waterlogged blocks cannot be pushed by pistons.
                This can block any ocean maker flying machine.
            """)
        val disableWaterloggedBlockPush: Boolean = false,
    ): BaseFeatureConfiguration()

}