package io.github.rothes.esu.bukkit.event

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import java.util.logging.Level

interface Nested {

    val parentPriority: EventPriority

    fun callNested() {
        val event = this as Event
        val handlers: HandlerList = event.handlers
        val listeners = handlers.getRegisteredListeners()

        for (registration in listeners) {
            if (registration.plugin.isEnabled && registration.priority == parentPriority) {
                try {
                    registration.callEvent(event)
                } catch (ex: Throwable) {
                    Bukkit.getServer().logger.log(
                        Level.SEVERE,
                        "Could not pass event ${event.getEventName()} to ${registration.plugin.name}",
                        ex
                    )
                }
            }
        }
    }

}