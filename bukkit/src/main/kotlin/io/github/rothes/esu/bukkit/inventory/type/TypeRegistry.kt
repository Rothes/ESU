package io.github.rothes.esu.bukkit.inventory.type

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import io.github.rothes.esu.bukkit.plugin
import org.bukkit.inventory.ItemStack

open class TypeRegistry<H: DynamicHolder<*>> {

    open val registry = hashMapOf<String, InventoryType<H>>()

    open fun register(vararg types: InventoryType<H>): TypeRegistry<H> {
        types.forEach { register(type = it) }
        return this
    }

    open fun register(type: InventoryType<H>): TypeRegistry<H> {
        registry[type.id] = type
        return this
    }

    fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
        val id = item.type?.lowercase() ?: error("InventoryItem type is null")
        val type = registry[id] ?: return item.item.item.also {
            plugin.warn("Unknown type '${item.type}'")
        }
        return type.parseType(slot, item, holder)
    }

}