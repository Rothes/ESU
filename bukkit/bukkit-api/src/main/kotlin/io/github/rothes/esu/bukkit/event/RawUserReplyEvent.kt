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

package io.github.rothes.esu.bukkit.event

import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList

class RawUserReplyEvent(
    player: Player,
    message: String,
    override var cancelledKt: Boolean,
    override val parentPriority: EventPriority,
): EsuUserEvent(player), CancellableKt, Nested {

    private var changed: Boolean = false

    var message: String = message
        set(value) {
            changed = true
            field = value
        }

    fun hasModified() = changed

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {
        const val REPLY_COMMANDS = "reply|r"

        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

}