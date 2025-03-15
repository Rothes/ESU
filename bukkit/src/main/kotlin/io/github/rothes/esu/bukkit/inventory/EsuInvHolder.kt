package io.github.rothes.esu.bukkit.inventory

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.util.ComponentUtils.miniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

abstract class EsuInvHolder<T>(val inventoryData: InventoryData<T>): InventoryHolder {

    private val inv: Inventory by lazy {
        val inv = if (inventoryData.size != null) {
            Bukkit.createInventory(this, inventoryData.size, inventoryData.title.miniMessage)
        } else if (inventoryData.inventoryType != null) {
            Bukkit.createInventory(this, inventoryData.inventoryType, inventoryData.title.miniMessage)
        } else {
            throw IllegalArgumentException("Both inventoryType and size are null in InventoryData")
        }

        fun setItem(slot: Int, char: Char) {
            val item = inventoryData.icons[char]
            if (item != null)
                inv.setItem(slot, parseAction(slot, item))
            else if (char != ' ')
                plugin.warn("Icon $char in layout ${inventoryData.layout} is not set!")
        }
        if (inventoryData.size != null) {
            var base = 0
            for (line in inventoryData.layout.split('\n').take(inventoryData.size / 9)) {
                var i = base
                for (c in line.take(9)) setItem(i++, c)
                base += 9
            }
        } else {
            for ((i, c) in inventoryData.layout.filterNot { it == '\n' }.withIndex()) setItem(i, c)
        }
        postInventoryBuild(inv)
        inv
    }

    fun open(humanEntity: HumanEntity) {
        Scheduler.schedule(humanEntity) {
            humanEntity.openInventory(inventory)
        }
    }

    fun close() {
        inventory.close()
    }

    open fun handleClick(e: InventoryClickEvent) {
        val clickedInventory = e.clickedInventory ?: return
        if (clickedInventory.holder == this) {
            handleClick(e.slot, e)
        } else {
            // We don't want players to move their items into our menu!
            if (e.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.isCancelled = true
            }
        }
    }

    open fun handleDrag(e: InventoryDragEvent) {
        if (e.rawSlots.find { it < inv.size } != null)
            e.isCancelled = true
    }

    protected open fun parseAction(slot: Int, item: InventoryData.InventoryItem): ItemStack? {
        return item.item.item
    }

    protected open fun handleClick(slot: Int, e: InventoryClickEvent) {
        e.isCancelled = true
    }

    protected open fun postInventoryBuild(inventory: Inventory) {}

    override fun getInventory(): Inventory = inv
}