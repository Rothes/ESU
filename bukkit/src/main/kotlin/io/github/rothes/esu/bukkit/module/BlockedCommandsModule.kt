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

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerCommandSendEvent
import org.bukkit.event.server.ServerCommandEvent
import org.bukkit.event.server.TabCompleteEvent

object BlockedCommandsModule: BukkitModule<BlockedCommandsModule.ModuleConfig, BlockedCommandsModule.ModuleLocale>() {

    override fun onEnable() {
        Listeners.register()
        if (ServerInfo.isPaper) PaperListeners.register()
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
        if (ServerInfo.isPaper) PaperListeners.unregister()
    }

    object Listeners: Listener {

        @EventHandler(ignoreCancelled = true)
        fun onCommand(event: PlayerCommandPreprocessEvent) {
            event.isCancelled = blocked(event.player.user, event.message.substring(1))
        }

        @EventHandler(ignoreCancelled = true)
        fun onCommand(event: ServerCommandEvent) {
            event.isCancelled = blocked(ConsoleUser, event.command)
        }

        @EventHandler
        fun onCommand(event: PlayerCommandSendEvent) {
            val user = event.player.user
            event.commands.removeIf {
                blocked(user, it, true)
            }
        }

        // This event is not calling at all on Folia (maybe Paper too)
        @EventHandler(ignoreCancelled = true)
        fun onCommand(event: TabCompleteEvent) {
            if (!event.isCommand) return
            val sender = event.sender
            val user = if (sender is Player) sender.user else ConsoleUser
            val buf = event.buffer.substring(1) // Remove '/' char
            event.completions.removeIf {
                blocked(user, buf + it, true)
            }
        }
    }

    object PaperListeners: Listener {

        @EventHandler(ignoreCancelled = true)
        fun onCommand(event: AsyncPlayerSendSuggestionsEvent) { // AsyncTabCompleteEvent may give empty completions (lazy init)
            val user = event.player.user
            val buf = event.buffer.substring(1) // Remove '/' char
            event.suggestions.list.removeIf {
                blocked(user, buf + it.text, true)
            }
        }
    }

    private fun blocked(user: BukkitUser, command: String, checkHide: Boolean = false): Boolean {
        val matched = config.blockingCommands.find { group ->
            if (group.consoleUserExcluded && user is ConsoleUser) {
                return@find false
            }

            if (checkHide && !group.hideCommand) {
                return@find false
            }

            group.commands.any { cmd ->
                cmd.containsMatchIn(command)
            }
        }
        if (matched != null) {
            if (!checkHide) {
                val key = matched.blockedMessage
                user.message(user.langOrNull(lang) { blockedMessage[key] } ?: key.message)
            }
            return true
        }
        return false
    }


    data class ModuleConfig(
        val blockingCommands: List<BlockingGroup> = arrayListOf(
            BlockingGroup("no-suicide", listOf("^(.+:)?suicide$".toRegex(), "^(.+:)?kill$".toRegex()))),
    ): BaseModuleConfiguration() {

        data class BlockingGroup(
            @Comment("The message key to send to users. You need to set the message in locale configs.")
            val blockedMessage: String = "",
            @Comment("The commands to block. Using regex.")
            val commands: List<Regex> = arrayListOf(),
            val consoleUserExcluded: Boolean = true,
            @Comment("Hide command from the command list send to player.")
            val hideCommand: Boolean = true,
        ): ConfigurationPart

    }

    data class ModuleLocale(
        val blockedMessage: Map<String, MessageData> = linkedMapOf(
            Pair("no-suicide", "<gold>Do not kill yourself! We still love you...".message)
        ),
    ): ConfigurationPart

}