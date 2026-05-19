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

import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.PlayerDeathEvent

class RichPlayerDeathEvent(
    private val parent: PlayerDeathEvent
): EsuUserEvent(parent.player, parent.isAsynchronous), CancellableKt {

    override var cancelledKt: Boolean
        get() = parent.isCancelled
        set(value) { parent.isCancelled = value }

    private var chatMessage: ((User) -> Component?)? = null

    fun setChatMessage(new: (receiver: User, old: Component?) -> Component?) {
        val old = this.chatMessage ?: parent.deathMessage()?.esu.let { { _ -> it } }
        this.chatMessage = { user ->
            new(user, old(user))
        }
    }

    override fun callEvent(): Boolean {
        val sp = super.callEvent()
        if (sp) finalizeCall()
        return sp
    }

    private fun finalizeCall() {
        val scope = chatMessage
        if (scope != null) {
            parent.deathMessage(null)
            for (user in BukkitUserManager.getUsers().filter { it.isOnline }.plus(ConsoleUser)) {
                scope(user)?.let { user.message(it) }
            }
        }
    }

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {

        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

}