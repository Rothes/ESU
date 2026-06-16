package io.github.rothes.esu.bukkit.listener

import io.github.rothes.esu.bukkit.util.inventory.InventoryUtils.esuHolder
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

object EsuInvBaseListeners : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(e: InventoryClickEvent) {
        val holder = e.inventory.esuHolder ?: return
        holder.handleClick(e)
    }
    @EventHandler(priority = EventPriority.LOWEST)
    fun onDrag(e: InventoryDragEvent) {
        val holder = e.inventory.esuHolder ?: return
        holder.handleDrag(e)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onClose(e: InventoryCloseEvent) {
        val holder = e.inventory.esuHolder ?: return
        holder.onClose()
    }

}
