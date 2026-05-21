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

package io.github.rothes.esu.velocity.user

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.core.user.UserManager
import java.util.*

object VelocityUserManager: UserManager<CommandSource, PlayerUser>() {

    init {
        instance = VelocityUserManager
    }

    override fun get(native: CommandSource): PlayerUser {
        val player = native.asPlayer
        val uuid = player.uniqueId
        return getCache(uuid)?.also { it.playerCache = player } ?: PlayerUser(player).also { set(uuid, it) }
    }

    override fun create(uuid: UUID): PlayerUser = PlayerUser(uuid)

    override fun unload(native: CommandSource): PlayerUser? = unload(native.asPlayer.uniqueId)

    private val CommandSource.asPlayer: Player
        get() = this as Player

}