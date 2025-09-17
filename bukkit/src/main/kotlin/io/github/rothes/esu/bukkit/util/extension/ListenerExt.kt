package io.github.rothes.esu.bukkit.util.extension

import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

object ListenerExt {

    fun Listener.register(plugin: Plugin = io.github.rothes.esu.bukkit.plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun Listener.unregister() {
        HandlerList.unregisterAll(this)
    }

}