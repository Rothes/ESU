/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.vanillatweaks

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.papermc.paper.event.player.PlayerOpenSignEvent
import net.kyori.adventure.text.Component
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.HangingSign
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

object SignBlockFeature: CommonFeature<Unit, Unit>() {

    init {
        registerFeature(DisableEditingSignText)
    }

    override fun onEnable() {}

    object DisableEditingSignText: CommonFeature<DisableEditingSignText.FeatureConfig, Unit>() {

        override fun checkUnavailable(): Feature.AvailableCheck? {
            return super.checkUnavailable() ?: let {
                if (!ServerInfo.isPaper)
                    return errFail { "Requires Paper".message }
                null
            }
        }

        override fun onEnable() {
            Listeners.register()
        }

        override fun onDisable() {
            super.onDisable()
            Listeners.unregister()
        }

        private object Listeners : Listener {

            @EventHandler(ignoreCancelled = true)
            fun onOpenSign(e: PlayerOpenSignEvent) {
                val sign = e.sign
                fun handleBlocked() {
                    e.isCancelled = true
                    val sound =
                        if (sign is HangingSign) Sound.BLOCK_HANGING_SIGN_WAXED_INTERACT_FAIL else Sound.BLOCK_SIGN_WAXED_INTERACT_FAIL
                    sign.world.playSound(sign.block.location, sound, SoundCategory.BLOCKS, 1f, 1f)
                }

                val lines = sign.getSide(e.side).lines()
                if (config.allowEditingEmptyLines) {
                    if (lines.all { it.notEmpty() }) handleBlocked()
                } else {
                    if (lines.any { it.notEmpty() }) handleBlocked()
                }
            }

            @EventHandler
            fun onSignEdit(e: SignChangeEvent) {
                val block = e.block.state as Sign
                val oldLines = block.getSide(e.side).lines()
                val newLines = e.lines()
                for ((index, component) in oldLines.withIndex()) {
                    if (component.notEmpty()) {
                        newLines[index] = component
                    }
                }
            }

//        @EventHandler
//        fun onEdit(e: PlayerInteractEvent) {
//            if (e.action != Action.RIGHT_CLICK_BLOCK) return
//            val block = e.clickedBlock ?: return
//            val item = e.item ?: return
//            item as CraftItemStack
//            val handle = item.handle
//            if (handle.item is SignApplicator) return
//
//            val sign = block.state as? Sign ?: return
//
//        }

            private fun Component.notEmpty() = this != Component.empty()

        }


        data class FeatureConfig(
            val allowEditingEmptyLines: Boolean = true,
        ) : BaseFeatureConfiguration()
    }

}