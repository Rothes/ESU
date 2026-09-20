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

import io.github.rothes.esu.bukkit.event.Nested.Companion.hasListener
import io.github.rothes.esu.bukkit.util.ServerInfo
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
class BlockStateRegistryChangedEvent(override val parentPriority: EventPriority) : Event(true), Nested {

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {

        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            if (ServerInfo.PluginEnabled.CraftEngine) {
                CraftEngineHook.register()
            }
        }

        private object CraftEngineHook {
            fun register() {
                Nested.registerNested(CraftEngineReloadEvent::class.java) { _, priority ->
                    if (handlers.hasListener(priority)) BlockStateRegistryChangedEvent(priority).callNested()
                }
            }
        }
    }

}