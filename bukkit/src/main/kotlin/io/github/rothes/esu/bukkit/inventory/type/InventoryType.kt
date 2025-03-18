package io.github.rothes.esu.bukkit.inventory.type

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.inventory.ItemStack

interface InventoryType<H: DynamicHolder<*>> {

    val name: String
    val id: String
        get() = name.lowercase()

    fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack?

}