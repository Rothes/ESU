package io.github.rothes.esu.bukkit.inventory.type

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.inventory.ItemStack

open class SimpleType<H: DynamicHolder<*>>(
    final override val name: String
): InventoryType<H> {

    override fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
        return item.item.item
    }

    override fun toString(): String {
        return "SimpleType(name='$name')"
    }

    companion object {
        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int,
            item: InventoryData.InventoryItem,
            holder: H,
        ) -> ItemStack?): SimpleType<H> {
            return object : SimpleType<H>(name) {
                override fun parseType(
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
        ) -> ItemStack?): SimpleType<H> {
            return object : SimpleType<H>(name) {
                override fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                    return func(slot, item)
                }
            }
        }
    }

}
