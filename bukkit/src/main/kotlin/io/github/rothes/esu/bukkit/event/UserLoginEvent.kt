package io.github.rothes.esu.bukkit.event

import fr.xephi.authme.api.v3.AuthMeApi
import fr.xephi.authme.events.LoginEvent
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import org.bukkit.Bukkit
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
            if (Bukkit.getPluginManager().isPluginEnabled("Authme")) {
                Bukkit.getPluginManager().registerEvents(object : Listener {
                    @EventHandler
                    fun onLogin(e: LoginEvent) {
                        Bukkit.getPluginManager().callEvent(UserLoginEvent(e.player.user))
                    }
                }, plugin)
                Bukkit.getOnlinePlayers()
                    .filter { AuthMeApi.getInstance().isAuthenticated(it) }
                    .forEach { it.user.logonBefore = true }
            } else {
                Bukkit.getPluginManager().registerEvents(object : Listener {
                    @EventHandler
                    fun onLogin(e: PlayerJoinEvent) {
                        Bukkit.getPluginManager().callEvent(UserLoginEvent(e.player.user))
                    }
                }, plugin)
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