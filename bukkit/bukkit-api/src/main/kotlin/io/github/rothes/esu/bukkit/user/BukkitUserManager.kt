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

package io.github.rothes.esu.bukkit.user

import io.github.rothes.esu.core.user.UserManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

object BukkitUserManager: UserManager<CommandSender, PlayerUser>() {

    init {
        instance = BukkitUserManager
    }

    override fun get(native: CommandSender): PlayerUser {
        val player = native.asPlayer
        val uuid = player.uniqueId
        return getCache(uuid)?.also { it.playerCache = player } ?: PlayerUser(player).also { set(uuid, it) }
    }

    override fun create(uuid: UUID): PlayerUser = PlayerUser(uuid)

    override fun unload(native: CommandSender): PlayerUser? = unload(native.asPlayer.uniqueId)

    private val CommandSender.asPlayer: Player
        get() = this as Player

}