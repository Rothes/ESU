package io.github.rothes.esu.bukkit.module.core

import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.unregister
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.concurrent.ConcurrentHashMap

object CoreController {

    private val lastMoveTime = ConcurrentHashMap<Player, Long>() // Folia

    fun onEnable() {
        Listeners.register()
        val now = System.currentTimeMillis()
        for (p in Bukkit.getOnlinePlayers()) {
            lastMoveTime[p] = now
        }
    }

    fun onDisable() {
        Listeners.unregister()
        lastMoveTime.clear()
    }

    object RunningProvider: Provider {

        override val isEnabled: Boolean = true

        override fun lastMoveTime(player: Player): Long {
            return lastMoveTime[player] ?: System.currentTimeMillis()
        }

    }

    private object Listeners: Listener {

        @EventHandler
        fun onPlayerMove(event: PlayerMoveEvent) {
            val p = event.player
            lastMoveTime[p] = System.currentTimeMillis()
        }

        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            val p = event.player
            lastMoveTime[p] = System.currentTimeMillis()
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            val p = event.player
            lastMoveTime.remove(p)
        }

    }

}