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

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v21_6

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeHandler
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import io.github.rothes.esu.core.util.UnsafeUtils.usLongAccessor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.TicketType

object TicketTypeHandlerImpl: TicketTypeHandler {

    private val KEY_HANDLER by Versioned(ResourceKeyHandler::class.java)

    val map = BuiltInRegistries.TICKET_TYPE
        .entrySet()
        .associate {
            val name = KEY_HANDLER.getResourceKeyString(it.key)
            name to wrapTicketType(it.value, name)
        }

    override fun getTicketTypeMap(): Map<String, TicketTypeHandler.NmsTicketType> {
        return map
    }

    private fun wrapTicketType(handle: TicketType<*>, name: String): TicketTypeHandler.NmsTicketType {
        return if (ServerInfo.isPaper) NmsTicketTypeMoonriseImpl(handle, name) else NmsTicketTypeCBImpl(handle, name)
    }

    class NmsTicketTypeMoonriseImpl(
        override val handle: TicketType<*>,
        override val name: String,
    ): TicketTypeHandler.NmsTicketType {

        override var expiryTicks: Long
            get() = handle.timeout()
            set(value) {
                handle.`moonrise$setTimeout`(value)
            }
    }

    class NmsTicketTypeCBImpl(
        override val handle: TicketType<*>,
        override val name: String,
    ): TicketTypeHandler.NmsTicketType {

        override var expiryTicks: Long
            get() = handle.timeout()
            set(value) {
                TIMEOUT[handle] = value
            }

        companion object {
            private val TIMEOUT = TicketType::class.java.getDeclaredField("timeout").usLongAccessor
        }
    }

}