package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.inventory.ItemStack

open class SimpleAction<H: DynamicHolder<*>>(
    final override val name: String
): InventoryAction<H> {

    override val id: String = "[${name.lowercase()}]"

    override fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
        return item.item.item
    }

    companion object {
        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int,
            item: InventoryData.InventoryItem,
            holder: H,
        ) -> ItemStack?): SimpleAction<H> {
            return object : SimpleAction<H>(name) {
                override fun parseAction(
                    slot: Int,
                    item: InventoryData.InventoryItem,
                    holder: H,
                ): ItemStack? {
                    return func(slot, item, holder)
                }
            }
        }

        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int,
            item: InventoryData.InventoryItem,
        ) -> ItemStack?): SimpleAction<H> {
            return object : SimpleAction<H>(name) {
                override fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                    return func(slot, item)
                }
            }
        }
    }

}
