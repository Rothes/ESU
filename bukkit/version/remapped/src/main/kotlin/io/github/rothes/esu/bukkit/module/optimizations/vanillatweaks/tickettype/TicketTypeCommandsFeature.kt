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

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.extension.math.floorI
import org.bukkit.World
import org.incendo.cloud.annotations.Command

object TicketTypeCommandsFeature: CommonFeature<FeatureToggle.DefaultFalse, Unit>() {

    override fun onEnable() {
        val handler = versioned<ChunkTicketHandler>()

        registerCommands(object {
            @Command("esu optimizations ticketType getTickets [chunkX] [chunkZ] [world]")
            @ShortPerm
            fun getTickets(sender: User,
                           chunkX: Int = floorI(sender.player.x) shr 4, chunkZ: Int = floorI(sender.player.z) shr 4,
                           world: World = sender.player.world) {
                handler.sendTicketDebugString(sender, chunkX, chunkZ, world)
            }

            private val User.player
                get() = (this as PlayerUser).player
        })
    }

    interface ChunkTicketHandler {

        fun sendTicketDebugString(user: User, chunkX: Int, chunkZ: Int, world: World)

    }

}
