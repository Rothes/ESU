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

package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.user.User
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit

object CommonActions {

    val CHAT = ArgumentAction.create("Chat") { user, arg ->
        arg ?: return@create
        (user as PlayerUser).player.chat(arg.parsed(user))
    }
    val CLOSE = SimpleAction.create("Close") { user ->
        (user as? PlayerUser)?.player?.closeInventory()
    }
    val COMMAND = ArgumentAction.create("Command") { user, arg ->
        arg ?: return@create
        Bukkit.dispatchCommand((user as BukkitUser).commandSender, arg.parsed(user))
    }
    val CONSOLE = ArgumentAction.create("Console") { user, arg ->
        arg ?: return@create
        Bukkit.dispatchCommand(ConsoleUser.commandSender, arg.parsed(user))
    }
    val MESSAGE = ArgumentAction.create("Message") { user, arg ->
        arg ?: return@create
        user.message(arg.parsed(user).message)
    }

    private fun String.parsed(user: User): String {
        return if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this
        } else if (user is PlayerUser) {
            PlaceholderAPI.setPlaceholders(user.player, this)
        } else {
            PlaceholderAPI.setPlaceholders(null, this)
        }
    }

}