/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.optimizations.antilag

import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
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
        }
    }

    data class FeatureConfig(
        @Comment("Enable this will disable water spread from waterlogged blocks.")
        val disableWaterSpread: Boolean = false,
        @Comment("""
                If enabled, waterlogged blocks cannot be pushed by pistons.
                This can block any ocean maker flying machine.
            """)
        val disableWaterloggedBlockPush: Boolean = false,
    ): BaseFeatureConfiguration()

}