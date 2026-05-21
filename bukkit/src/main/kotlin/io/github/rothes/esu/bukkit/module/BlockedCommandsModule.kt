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

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.server.ServerCommandEvent

object BlockedCommandsModule: BukkitModule<BlockedCommandsModule.ModuleConfig, BlockedCommandsModule.ModuleLocale>() {

    override fun onEnable() {
        Listeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
    }

    object Listeners: Listener {

        @EventHandler
        fun onCommand(event: PlayerCommandPreprocessEvent) {
            event.isCancelled = event.isCancelled || blocked(event.player.user, event.message.substring(1))
        }

        @EventHandler
        fun onCommand(event: ServerCommandEvent) {
            event.isCancelled = event.isCancelled || blocked(ConsoleUser, event.command)
        }

        private fun blocked(user: BukkitUser, command: String): Boolean {
            val matched = config.blockingCommands.find { group ->
                if (group.consoleUserExcluded && user is ConsoleUser) {
                    return@find false
                }

                group.commands.any { cmd ->
                    cmd.containsMatchIn(command)
                }
            }
            if (matched != null) {
                val key = matched.blockedMessage
                user.message(user.localedOrNull(lang) { blockedMessage[key] } ?: key.message)
                return true
            }
            return false
        }
    }


    data class ModuleConfig(
        val blockingCommands: List<BlockingGroup> = arrayListOf(BlockingGroup("no-suicide", listOf("^(.+:)?suicide$".toRegex(), "^(.+:)?kill$".toRegex()))),
    ): BaseModuleConfiguration() {

        data class BlockingGroup(
            @Comment("The message key to send to users. You need to set the message in locale configs.")
            val blockedMessage: String = "",
            @Comment("The commands to block. Using regex.")
            val commands: List<Regex> = arrayListOf(),
            val consoleUserExcluded: Boolean = true,
        ): ConfigurationPart

    }

    data class ModuleLocale(
        val blockedMessage: Map<String, MessageData> = linkedMapOf(
            Pair("no-suicide", "<gold>Do not kill yourself! We still love you...".message)
        ),
    ): ConfigurationPart

}