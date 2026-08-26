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

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.logging.Level

interface Nested {

    val parentPriority: EventPriority

    fun callNested() {
        val event = this as Event
        val handlers = event.handlers
        val listeners = handlers.getRegisteredListeners()

        for (registration in listeners) {
            if (registration.plugin.isEnabled && registration.priority == parentPriority) {
                try {
                    registration.callEvent(event)
                } catch (ex: Throwable) {
                    Bukkit.getServer().logger.log(
                        Level.SEVERE,
                        "Could not pass event ${event.getEventName()} to ${registration.plugin.name}",
                        ex
                    )
                }
            }
        }
    }

    companion object {

        fun HandlerList.hasListener(priority: EventPriority): Boolean {
            // Can optimize by using handlerslots field, if necessary
            for (listener in registeredListeners) {
                if (listener.priority == priority) return true
            }
            return false
        }

        fun <T: Event> registerNested(event: Class<T>, plugin: Plugin = io.github.rothes.esu.bukkit.plugin, executor: NestedEventExecutor<T>) {
            for (priority in EventPriority.entries) {
                Bukkit.getPluginManager().registerEvent(
                    event,
                    EmptyListener,
                    priority,
                    { listener, event ->
                        @Suppress("UNCHECKED_CAST")
                        event as T
                        executor.execute(event, priority)
                    },
                    plugin
                )
            }
        }

        fun interface NestedEventExecutor<T> {
            fun execute(event: T, priority: EventPriority)
        }

        private object EmptyListener : Listener
    }

}