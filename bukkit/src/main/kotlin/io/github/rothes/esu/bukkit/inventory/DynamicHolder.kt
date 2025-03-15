package io.github.rothes.esu.bukkit.inventory

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.action.ActionRegistry
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class DynamicHolder<T>(inventoryData: InventoryData<T>): EsuInvHolder<T>(inventoryData) {

    private val defers = PriorityQueue<DeferTask>(compareByDescending { it.priority })
    private val click = Int2ReferenceOpenHashMap<(InventoryClickEvent) -> Unit>()

    protected val actionRegistry: ActionRegistry<DynamicHolder<T>> = ActionRegistry.base()

    fun defer(priority: Int = 0, function: Inventory.() -> Unit): ItemStack? {
        defers.add(DeferTask(priority, function))
        return null
    }

    fun click(slot: Int, function: (InventoryClickEvent) -> Unit) {
        click[slot] = function
    }

    override fun postInventoryBuild(inventory: Inventory) {
        defers.forEach { it.task(inventory) }
        defers.clear()
    }

    override fun handleClick(slot: Int, e: InventoryClickEvent) {
        super.handleClick(slot, e)
        click[slot]?.let { it(e) }
    }

    override fun parseAction(slot: Int, item: InventoryData.InventoryItem): ItemStack? {
        if (item.action != null) {
            return actionRegistry.parseAction(slot, item, this)
        }
        return super.parseAction(slot, item)
    }

    private class DeferTask(val priority: Int, val task: Inventory.() -> Unit)

}