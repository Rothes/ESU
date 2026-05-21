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

import io.github.rothes.esu.bukkit.module.essentialcommands.*
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration

object EssentialCommandsModule: BukkitModule<BaseModuleConfiguration, EssentialCommandsModule.ModuleLang>() {

    init {
        listOf(
            ClientLocale, DimensionTravel, EnderChest,
            Feed, Heal, Invulnerable, Ip,
            IpGroup, Kill, NoFall,
            OpenInventory, Ping,
            PlayerChunkTickets, Spectate,
            Speed, Suicide, TpChunk
        ).forEach { cmd -> registerFeature(cmd) }
    }

    override fun onEnable() {}

    data class ModuleLang(
        val unsafeTeleportSpot: MessageData = "<ec>Cannot find a safe spot for teleport.".message,
        val teleportingPlayer: MessageData = "<tc>Teleporting <tdc><player></tdc>...".message,
        val teleportFailedUnknown: MessageData = "<ec>Failed to teleport.".message,
    ): ConfigurationPart

}