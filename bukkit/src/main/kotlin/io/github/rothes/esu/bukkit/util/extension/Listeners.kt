package io.github.rothes.esu.bukkit.util.extension

import io.github.rothes.esu.bukkit.bootstrap
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

fun Listener.register(plugin: Plugin = bootstrap) {
    Bukkit.getPluginManager().registerEvents(this, plugin)
}

fun Listener.unregister() {
    HandlerList.unregisterAll(this)
}