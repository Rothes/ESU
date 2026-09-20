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

import io.github.rothes.esu.bukkit.module.anticheat.ExploitCheats
import io.github.rothes.esu.bukkit.module.anticheat.MovementCheats
import io.github.rothes.esu.bukkit.module.anticheat.PrePacketEventManager
import io.github.rothes.esu.bukkit.module.anticheat.TransactionCheats
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration

object AntiCheatModule : BukkitModule<BaseModuleConfiguration, Unit>() {

    init {
        registerFeature(ExploitCheats)
        registerFeature(MovementCheats)
        registerFeature(TransactionCheats)
    }

    override fun onEnable() {
        if (ServerInfo.PluginEnabled.PacketEvents) PrePacketEventManager.inject()
    }

    override fun onDisable() {
        super.onDisable()
        if (ServerInfo.PluginEnabled.PacketEvents) PrePacketEventManager.eject()
    }

}