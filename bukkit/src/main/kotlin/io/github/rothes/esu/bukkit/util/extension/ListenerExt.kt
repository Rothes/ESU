package io.github.rothes.esu.bukkit.util.extension

import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

object ListenerExt {

    fun Listener.unregister() {
        HandlerList.unregisterAll(this)
    }

}