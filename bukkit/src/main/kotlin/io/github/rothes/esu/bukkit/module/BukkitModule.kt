package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.CommonModule
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

abstract class BukkitModule<C: ConfigurationPart, L: ConfigurationPart> : CommonModule<C, L>() {

    protected val registeredListeners = linkedSetOf<Listener>()

    override fun onDisable() {
        unregisterListeners()
        super.onDisable()
    }

    fun registerListener(listener: Listener, plugin: JavaPlugin = io.github.rothes.esu.bukkit.bootstrap) {
        Bukkit.getPluginManager().registerEvents(listener, plugin)
        registeredListeners.add(listener)
    }

    fun unregisterListeners() {
        registeredListeners.removeIf {
            HandlerList.unregisterAll(it)
            true
        }
    }

    fun log(msg: String) {
        ConsoleUser.log("[$name] $msg]")
    }

}