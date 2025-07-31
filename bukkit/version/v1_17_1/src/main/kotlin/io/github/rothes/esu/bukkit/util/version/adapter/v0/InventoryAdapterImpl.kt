package io.github.rothes.esu.bukkit.util.version.adapter.v0

import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView

class InventoryAdapterImpl: InventoryAdapter {

    override fun getTopInventory(inventoryView: InventoryView): Inventory {
        return inventoryView.topInventory
    }

}