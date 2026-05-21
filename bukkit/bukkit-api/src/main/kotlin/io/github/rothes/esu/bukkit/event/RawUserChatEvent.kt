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

package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.lib.adventure.text.Component
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerEvent

class RawUserChatEvent(
    async: Boolean,
    player: Player,
    message: Component,
    override var cancelledKt: Boolean,
    override val parentPriority: EventPriority
): EsuUserEvent(player, async), CancellableKt, Nested {

    private var changed: Boolean = false

    var message: Component = message
        set(value) {
            changed = true
            field = value
        }

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {

        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            fun <T> callChatEvent(
                event: T,
                message: Component,
                priority: EventPriority
            ): RawUserChatEvent where T : PlayerEvent, T : Cancellable {
                val esuEvent =
                    RawUserChatEvent(event.isAsynchronous, event.player, message, event.isCancelled, priority)
                esuEvent.callNested()

                event.isCancelled = esuEvent.isCancelled
                return esuEvent
            }

            try {
                AsyncChatEvent::class.java.toString()
                object : Listener {
                    fun callPaper(event: AsyncChatEvent, priority: EventPriority) {
                        val esu = callChatEvent(event, event.message().esu, priority)
                        if (esu.changed) event.message(esu.message.server)
                    }

                    @EventHandler(priority = EventPriority.LOWEST)
                    fun onChat0(event: AsyncChatEvent) {
                        callPaper(event, EventPriority.LOWEST)
                    }
                    @EventHandler(priority = EventPriority.LOW)
                    fun onChat1(event: AsyncChatEvent) {
                        callPaper(event, EventPriority.LOW)
                    }
                    @EventHandler(priority = EventPriority.NORMAL)
                    fun onChat2(event: AsyncChatEvent) {
                        callPaper(event, EventPriority.NORMAL)
                    }
                    @EventHandler(priority = EventPriority.HIGH)
                    fun onChat3(event: AsyncChatEvent) {
                        callPaper(event, EventPriority.HIGH)
                    }
                    @EventHandler(priority = EventPriority.HIGHEST)
                    fun onChat4(event: AsyncChatEvent) {
                        callPaper(event, EventPriority.HIGHEST)
                    }
                }
            } catch (_: NoClassDefFoundError) {
                @Suppress("DEPRECATION")
                object : Listener {
                    fun callCb(event: AsyncPlayerChatEvent, priority: EventPriority) {
                        val esu = callChatEvent(event, event.message.legacy, priority)
                        if (esu.changed) event.message = esu.message.legacy
                    }

                    @EventHandler(priority = EventPriority.LOWEST)
                    fun onChat0(event: AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.LOWEST)
                    }
                    @EventHandler(priority = EventPriority.LOW)
                    fun onChat1(event: AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.LOW)
                    }
                    @EventHandler(priority = EventPriority.NORMAL)
                    fun onChat2(event: AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.NORMAL)
                    }
                    @EventHandler(priority = EventPriority.HIGH)
                    fun onChat3(event: AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.HIGH)
                    }
                    @EventHandler(priority = EventPriority.HIGHEST)
                    fun onChat4(event: AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.HIGHEST)
                    }
                }
            }.register()
        }

    }

}