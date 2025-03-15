package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.inventory.ItemStack

interface InventoryAction<H: DynamicHolder<*>> {

    val name: String
    val id: String
        get() = name.lowercase()

    fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack?

    companion object {
        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int, item: InventoryData.InventoryItem, holder: H
        ) -> ItemStack?): InventoryAction<H> {
            return object : InventoryAction<H> {
                override val name: String = name
                override fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                    return func(slot, item, holder)
                }
            }
        }

        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int, item: InventoryData.InventoryItem
        ) -> ItemStack?): InventoryAction<H> {
            return object : InventoryAction<H> {
                override val name: String = name
                override fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                    return func(slot, item)
                }
            }
        }
    }

}