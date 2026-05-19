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

import fr.xephi.authme.api.v3.AuthMeApi
import fr.xephi.authme.events.LoginEvent
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Called when a player user authenticates and logins their account.
 */
class UserLoginEvent(
    player: Player
): EsuUserEvent(player) {

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {
        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            fun reg(listener: Listener, filter: (Player) -> Boolean) {
                Bukkit.getPluginManager().registerEvents(listener, plugin)
                Bukkit.getOnlinePlayers().filter(filter).forEach { it.user.logonBefore = true }
            }
            if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
                reg(object : Listener {
                    @EventHandler
                    fun onLogin(e: LoginEvent) {
                        Bukkit.getPluginManager().callEvent(UserLoginEvent(e.player))
                    }
                }) { AuthMeApi.getInstance().isAuthenticated(it) }
            } else {
                reg(object : Listener {
                    @EventHandler
                    fun onLogin(e: PlayerJoinEvent) {
                        Bukkit.getPluginManager().callEvent(UserLoginEvent(e.player))
                    }
                }) { true }
            }
            // Internal event
            Bukkit.getPluginManager().registerEvents(object : Listener {
                @EventHandler
                fun onLogin(e: UserLoginEvent) {
                    e.user.logonBefore = true
                }
            }, plugin)
        }
    }
}