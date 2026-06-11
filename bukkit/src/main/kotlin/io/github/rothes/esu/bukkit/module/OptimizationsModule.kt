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

package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.optimizations.AntiLagFeatures
import io.github.rothes.esu.bukkit.module.optimizations.VanillaTweaksFeatures
import io.github.rothes.esu.core.configuration.LoadedConfiguration
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object OptimizationsModule: BukkitModule<OptimizationsModule.ModuleConfig, EmptyConfiguration>() {

    init {
        registerFeature(AntiLagFeatures)
        registerFeature(VanillaTweaksFeatures)
    }

    override fun onEnable() { }

    override fun preprocessConfig(configuration: LoadedConfiguration) {
        // v0.14.0
        val node = configuration.node
        val list = listOf(
            "ticket-type" to "vanilla-tweaks",
            "waterlogged" to "anti-lag",
        )
        for ((key, value) in node.childrenMap()) {
            val pair = list.find { it.first == key } ?: continue
            node.node(pair.second, pair.first).from(value)
            node.removeChild(key)
        }
    }

    class ModuleConfig : BaseModuleConfiguration()

}