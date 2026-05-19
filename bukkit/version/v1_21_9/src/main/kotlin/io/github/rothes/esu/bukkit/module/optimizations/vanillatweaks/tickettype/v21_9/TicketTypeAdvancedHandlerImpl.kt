/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v21_9

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeAdvancedHandler
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import net.minecraft.server.level.TicketType

object TicketTypeAdvancedHandlerImpl : TicketTypeAdvancedHandler {

    private val FLAG_ACCESSOR = TicketType::class.java.getDeclaredField("flags").usIntAccessor

    override var TicketType<*>.persist: Boolean
        get() = persist()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111111110
            FLAG_ACCESSOR[this] = if (value) current or 0b1 else current
        }
    override var TicketType<*>.loadsChunk: Boolean
        get() = doesLoad()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111111101
            FLAG_ACCESSOR[this] = if (value) current or 0b10 else current
        }
    override var TicketType<*>.ticksChunk: Boolean
        get() = doesSimulate()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111111011
            FLAG_ACCESSOR[this] = if (value) current or 0b100 else current
        }
    override var TicketType<*>.keepsDimensionActive: Boolean
        get() = shouldKeepDimensionActive()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111110111
            FLAG_ACCESSOR[this] = if (value) current or 0b1000 else current
        }
    override var TicketType<*>.expiresIfUnloaded: Boolean
        get() = canExpireIfUnloaded()
        set(value) {
            val current = FLAG_ACCESSOR[this] and 0b1111111111111111111111111101111
            FLAG_ACCESSOR[this] = if (value) current or 0b10000 else current
        }

}