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

package io.github.rothes.esu.bukkit.module.core

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.module.CoreModule
import io.github.rothes.esu.bukkit.module.core.persistence.CorePersistentStorage
import io.github.rothes.esu.bukkit.module.core.persistence.PersistentData
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.extension.unregister
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object CoreController {

    fun onEnable() {
        if (CoreModule.config.persistentStorage.enabled) CorePersistentStorage // Init table
        Listeners.register()
        val now = System.currentTimeMillis()
        for (p in Bukkit.getOnlinePlayers()) {
            if (tryLoadPersistentData(p)) continue
            RunningProviders.moveTime.map.putIfAbsent(p, now)
            RunningProviders.posMoveTime.map.putIfAbsent(p, now)
            RunningProviders.attackTime.map.putIfAbsent(p, now)
        }
    }

    fun onDisable() {
        Listeners.unregister()
        for (player in Bukkit.getOnlinePlayers()) {
            savePersistentData(player, false)
        }
        RunningProviders.moveTime.map.clear()
        RunningProviders.posMoveTime.map.clear()
        RunningProviders.attackTime.map.clear()
        RunningProviders.genericActiveTime.map.clear()
    }

    private fun tryLoadPersistentData(player: Player): Boolean {
        if (!CoreModule.config.persistentStorage.enabled) return false

        val persistent = CorePersistentStorage.loadUserData(player.user) ?: return false

        with(RunningProviders) {
            if (persistent.lastActionDuration != null) {
                val now = System.currentTimeMillis()
                with(persistent.lastActionDuration) {
                    attackTime.map[player] = now - attack
                    genericActiveTime.map[player] = now - generic
                    moveTime.map[player] = now - move
                    posMoveTime.map[player] = now - posMove
                }
            } else {
                attackTime.map[player] = persistent.attackTime
                genericActiveTime.map[player] = persistent.genericActiveTime
                moveTime.map[player] = persistent.moveTime
                posMoveTime.map[player] = persistent.posMoveTime
            }
        }
        return true
    }

    private fun savePersistentData(player: Player, async: Boolean) {
        if (!CoreModule.config.persistentStorage.enabled) return

        val now = System.currentTimeMillis()
        val persistent = with(RunningProviders) {
            PersistentData(
                lastActionDuration = PersistentData.LastActionDuration(
                    attack = now - moveTime[player],
                    generic = now - genericActiveTime[player],
                    move = now - moveTime[player],
                    posMove = now - posMoveTime[player],
                ),
            )
        }
        if (async)
            CorePersistentStorage.saveUserData(player.user, persistent)
        else
            CorePersistentStorage.saveUserDataNow(player.user, persistent)
    }

    object RunningProviders: Providers {

        override val isEnabled: Boolean = true

        override val attackTime = MapPlayerTimeProvider()
        override val posMoveTime = MapPlayerTimeProvider()
        override val moveTime = MapPlayerTimeProvider()

        override val genericActiveTime = MapPlayerTimeProvider().also {
            val listener = object : PlayerTimeProvider.ChangeListener {
                override fun onTimeChanged(player: Player, oldTime: Long, newTime: Long) {
                    it[player] = newTime
                }
            }
            attackTime.registerListener(listener)
            posMoveTime.registerListener(listener)
            moveTime.registerListener(listener)
        }

    }

    private object Listeners: Listener {

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            val p = event.player
            val now = System.currentTimeMillis()
            RunningProviders.moveTime[p] = now
            if (event.from.world != event.to.world || event.from.distanceSquared(event.to) >= 1f / 128) {
                RunningProviders.posMoveTime[p] = now
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        fun onPlayerJoin(event: PlayerJoinEvent) {
            val p = event.player
            if (tryLoadPersistentData(p)) return
            val now = System.currentTimeMillis()
            RunningProviders.moveTime[p] = now
            RunningProviders.posMoveTime[p] = now
            RunningProviders.attackTime[p] = now
        }

        @EventHandler(priority = EventPriority.MONITOR)
        fun onPlayerQuit(event: PlayerQuitEvent) {
            val p = event.player
            savePersistentData(p, true)
            RunningProviders.attackTime.unload(p)
            RunningProviders.posMoveTime.unload(p)
            RunningProviders.moveTime.unload(p)
            RunningProviders.genericActiveTime.unload(p)
        }

        @EventHandler
        fun onDamage(event: EntityDamageByEntityEvent) {
            val damager = event.damager as? Player ?: return
            RunningProviders.attackTime[damager] = System.currentTimeMillis()
        }

    }

    class MapPlayerTimeProvider: PlayerTimeProvider {

        @get:ApiStatus.Internal
        val map = ConcurrentHashMap<Player, Long>()
        private val listeners = CopyOnWriteArrayList<PlayerTimeProvider.ChangeListener>()

        override fun get(player: Player): Long = map[player] ?: 0

        override fun set(player: Player, time: Long) {
            val old = map.put(player, time) ?: 0
            for (listener in listeners) {
                try {
                    listener.onTimeChanged(player, old, time)
                } catch (e: Throwable) {
                    core.err("An provider exception occurred:", e)
                }
            }
        }

        fun unload(player: Player) {
            map.remove(player)
        }

        override fun registerListener(listener: PlayerTimeProvider.ChangeListener) {
            listeners.add(listener)
        }

        override fun unregisterListener(listener: PlayerTimeProvider.ChangeListener) {
            listeners.remove(listener)
        }

    }

}