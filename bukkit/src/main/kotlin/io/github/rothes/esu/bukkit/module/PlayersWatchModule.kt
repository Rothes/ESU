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

package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object PlayersWatchModule: BukkitModule<PlayersWatchModule.ConfigData, PlayersWatchModule.ModuleLocale>() {

    val watching: ConcurrentHashMap<PlayerUser, WatchOptions> = ConcurrentHashMap()
    val players: LinkedHashSet<Player> = LinkedHashSet()

    override fun onEnable() {
        Listeners.register()

        Bukkit.getOnlinePlayers().forEach {
            it.spectatorTarget
        }
    }

    override fun onDisable() {
        super.onDisable()
        Listeners.unregister()
    }

    object Listeners: Listener {

        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            players.add(event.player)
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            players.remove(event.player)
        }
    }

    data class WatchOptions(
        var loop: Boolean = true,
        var onIndex: Int = (0 .. watching.size).random(),
    )

    data class ConfigData(
        @Comment("In ticks.")
        val switchWatchInterval: Long = 6 * 20
    ): BaseModuleConfiguration()

    data class ModuleLocale(
        val a: String = ""
    ): ConfigurationPart

}