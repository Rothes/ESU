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

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v1

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeHandler
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGet
import io.github.rothes.esu.core.util.UnsafeUtils.usLongAccessor
import net.minecraft.server.level.TicketType
import java.lang.reflect.Modifier

object TicketTypeHandlerImpl: TicketTypeHandler {

    private val expiryTicksField = TicketType::class.java.declaredFields
        .first { it.type == Long::class.java && !Modifier.isStatic(it.modifiers) }
    private val expiryTicksFieldPrivate = Modifier.isPrivate(expiryTicksField.modifiers)

    val map = TicketType::class.java.declaredFields
        .filter { it.type.isAssignableFrom(TicketType::class.java) }
        .map { it.accessibleGet(null) as TicketType<*> }
        .map { wrapTicketType(it) }
        .associateBy { it.name }

    override fun getTicketTypeMap(): Map<String, TicketTypeHandler.NmsTicketType> {
        return map
    }

    private fun wrapTicketType(handle: TicketType<*>): TicketTypeHandler.NmsTicketType {
        return if (expiryTicksFieldPrivate) NmsTicketTypeCBImpl(handle) else NmsTicketTypePaperImpl(handle)
    }

    class NmsTicketTypePaperImpl(
        override val handle: TicketType<*>,
    ): TicketTypeHandler.NmsTicketType {

        override val name: String = handle.toString()

        override var expiryTicks: Long
            get() = handle.timeout
            set(value) {
                handle.timeout = value
            }
    }

    // On Spigot, it's not public
    class NmsTicketTypeCBImpl(
        override val handle: TicketType<*>,
    ): TicketTypeHandler.NmsTicketType {

        override val name: String = handle.toString()

        override var expiryTicks: Long
            get() = accessor[handle]
            set(value) {
                accessor[handle] = value
            }

        companion object {
            private val accessor = expiryTicksField.usLongAccessor
        }
    }

}