package io.github.rothes.esu.bukkit.event

import fr.xephi.authme.api.v3.AuthMeApi
import fr.xephi.authme.events.LoginEvent
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * Called when a player user authenticates and logins their account.
 */
class UserLoginEvent(
    val user: PlayerUser
): Event() {

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
            if (Bukkit.getPluginManager().isPluginEnabled("Authme")) {
                reg(object : Listener {
                    @EventHandler
                    fun onLogin(e: LoginEvent) {
                        Bukkit.getPluginManager().callEvent(UserLoginEvent(e.player.user))
                    }
                }) { AuthMeApi.getInstance().isAuthenticated(it) }
            } else {
                reg(object : Listener {
                    @EventHandler
                    fun onLogin(e: PlayerJoinEvent) {
                        Bukkit.getPluginManager().callEvent(UserLoginEvent(e.player.user))
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