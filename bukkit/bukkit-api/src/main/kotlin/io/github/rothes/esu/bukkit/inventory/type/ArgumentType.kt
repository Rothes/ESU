package io.github.rothes.esu.bukkit.inventory.type

import io.github.rothes.esu.bukkit.configuration.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.inventory.ItemStack

open class ArgumentType<H: DynamicHolder<*>>(
    final override val name: String
): InventoryType<H> {

    override fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
        return item.item.item
    }

    override fun toString(): String {
        return "ArgumentType(name='$name')"
    }

    protected val InventoryData.InventoryItem.arg
        get() = type?.substringAfter(']')?.removePrefix(" ")?.ifEmpty { null }

    companion object {
        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int,
            item: InventoryData.InventoryItem,
            arg: String?,
            holder: H,
        ) -> ItemStack?): ArgumentType<H> {
            return object : ArgumentType<H>(name) {
                override fun parseType(
                    slot: Int,
                    item: InventoryData.InventoryItem,
                    holder: H,
                ): ItemStack? {
                    return func(slot, item, item.arg, holder)
                }
            }
        }

        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int,
            item: InventoryData.InventoryItem,
            arg: String?,
        ) -> ItemStack?): ArgumentType<H> {
            return object : ArgumentType<H>(name) {
                override fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                    return func(slot, item, item.arg)
                }
            }
        }
    }

}
