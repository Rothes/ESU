package io.github.rothes.esu.bukkit.module.spawnprotect

import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import kotlin.math.abs

object WaterloggedFeature: CommonFeature<WaterloggedFeature.FeatureConfig, Unit>() {

    override fun onEnable() {
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
    }

    private object Listeners: Listener {

        @EventHandler
        fun onPistonPushWaterlogged(e: BlockPistonExtendEvent) {
            handleWaterloggedPush(e.blocks, e)
        }
        @EventHandler
        fun onPistonPushWaterlogged(e: BlockPistonRetractEvent) {
            handleWaterloggedPush(e.blocks, e)
        }

        private fun handleWaterloggedPush(blocks: List<Block>, e: BlockPistonEvent) {
            val config = config
            if (config.disableSpawnPushRadius > 0) {
                val dist = abs(e.block.x) + abs(e.block.z)
                if (dist <= config.disableSpawnPushRadius
                    && blocks.any { (it.blockData as? Waterlogged)?.isWaterlogged == true }) {
                    e.isCancelled = true
                }
            }
        }

    }


    data class FeatureConfig(
        @Comment("""
                Disable pushing waterlogged blocks in spawn circle range.
                set to -1 to disable the limit.
                This is to prevent ocean maker machine.
            """)
        val disableSpawnPushRadius: Long = -1
    )

}