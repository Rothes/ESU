package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.version.Versioned
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView

interface InventoryAdapter {

    fun getTopInventory(inventoryView: InventoryView): Inventory

    companion object {

        val instance by Versioned(InventoryAdapter::class.java)

        val InventoryView.topInv
            get() = instance.getTopInventory(this)

    }

}