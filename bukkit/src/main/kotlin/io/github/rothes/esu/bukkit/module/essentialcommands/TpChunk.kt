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

package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.command.parser.location.ChunkLocation
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerInfo.tp
import io.github.rothes.esu.bukkit.util.WorldUtils
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag

object TpChunk : BaseCommand<FeatureToggle.DefaultTrue, Unit>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("tpChunk <chunk> [world]")
            @ShortPerm
            suspend fun tpChunk(sender: User, chunk: ChunkLocation, world: World = (sender as PlayerUser).player.location.world) {
                tpChunk(sender, chunk, world, (sender as PlayerUser).player)
            }
            @Command("tpChunk <chunk> [world] [player]")
            @ShortPerm("others")
            suspend fun tpChunk(
                sender: User, chunk: ChunkLocation, world: World = (sender as PlayerUser).player.location.world,
                player: Player = (sender as PlayerUser).player,
                @Flag("unsafe") unsafe: Boolean = false
            ) {
                val location = player.location
                val target = Location(world, (chunk.chunkX shl 4) + 8.0, location.y, (chunk.chunkZ shl 4) + 8.0, location.yaw, location.pitch)
                val spot = WorldUtils.findStandableSpot(target, unsafe) ?: return sender.message(module.lang, { unsafeTeleportSpot })
                player.tp(spot)
                sender.message(module.lang, { teleportingPlayer }, player(player))
            }
        })
    }

}