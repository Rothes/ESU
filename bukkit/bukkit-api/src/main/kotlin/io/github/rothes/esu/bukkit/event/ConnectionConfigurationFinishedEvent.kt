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

@file:Suppress("DEPRECATION") // PlayerLoginEvent

package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.event.Nested.Companion.hasListener
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.lib.adventure.text.Component
import io.papermc.paper.connection.PaperPlayerConfigurationConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.jetbrains.annotations.ApiStatus
import java.util.*

@ApiStatus.Experimental
class ConnectionConfigurationFinishedEvent(
    val playerInformation: PlayerInformation,
    var kickMessage: Component?,
    override val parentPriority: EventPriority
) : Event(true), Nested {

    override fun getHandlers(): HandlerList = Companion.handlers

    data class PlayerInformation(
        val name: String,
        val uuid: UUID,
        val locale: String,
    )

    companion object {

        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            if (ServerInfo.isPaper && ServerInfo.mcVersion > "21.7") {
                PaperConnectionHook.register() // Avoid NoClassDefFoundError on unsupported servers
            } else {
                // Use AsyncPlayerPreLoginEvent on Spigot to avoid unnecessary processes.
                // Not possible to get player locale before player spawning to world.
                Nested.registerNested(AsyncPlayerPreLoginEvent::class.java) { event, priority ->
                    if (handlers.hasListener(priority)) {
                        val kickMessage = if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED) null else {
                            if (ServerInfo.isPaper) event.kickMessage().esu else event.kickMessage.legacy
                        }
                        val profile = PlayerInformation(event.name, event.uniqueId, ConsoleUser.clientLocale)
                        val e = ConnectionConfigurationFinishedEvent(profile, kickMessage, priority)
                        e.callNested()
                        val newKick = e.kickMessage
                        if (newKick !== kickMessage) {
                            if (newKick != null) {
                                if (ServerInfo.isPaper) event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, newKick.server)
                                else event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, newKick.legacy)
                            } else event.allow()
                        }
                    }
                }
            }
        }

        private object PaperConnectionHook {
            fun register() {
                @Suppress("UnstableApiUsage")
                Nested.registerNested(PlayerConnectionValidateLoginEvent::class.java) { event, priority ->
                    val connection = event.connection as? PaperPlayerConfigurationConnection ?: return@registerNested
                    if (handlers.hasListener(priority)) {
                        val kickMessage = event.kickMessage?.esu
                        val profile = PlayerInformation(connection.profile.name!!, connection.profile.id!!, connection.clientInformation.language)
                        val e = ConnectionConfigurationFinishedEvent(profile, kickMessage, priority)
                        e.callNested()
                        val newKick = e.kickMessage
                        if (newKick !== kickMessage) {
                            if (newKick != null) event.kickMessage(newKick.server)
                            else event.allow()
                        }
                    }
                }
            }
        }
    }

}