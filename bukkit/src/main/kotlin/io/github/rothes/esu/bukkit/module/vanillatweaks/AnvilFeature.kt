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

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.bukkit.util.version.adapter.AnvilInvAdapter.Companion.renameText
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.util.extension.charSize
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent

object AnvilFeature : CommonFeature<Unit, Unit>() {

    init {
        registerFeature(Renaming)
    }

    override fun onEnable() {
    }

    object Renaming: CommonFeature<Renaming.FeatureConfig, Unit>() {

        override fun onEnable() {
            Listeners.register(plugin)
        }

        override fun onDisable() {
            super.onDisable()
            Listeners.unregister()
        }

        private object Listeners: Listener {

            @EventHandler(priority = EventPriority.HIGHEST)
            fun onAnvilRename(e: PrepareAnvilEvent) {
                val renameText = e.renameText ?: return
                val size = renameText.charSize()
                val limit = config.maxRenameCharSize
                if (limit in 0 ..< size) {
                    e.result = null
                }
            }
        }

        data class FeatureConfig(
            val maxRenameCharSize: Int = -1,
        ) : BaseFeatureConfiguration()
    }

}