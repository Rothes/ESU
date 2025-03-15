package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import io.github.rothes.esu.bukkit.plugin
import org.bukkit.inventory.ItemStack

open class ActionRegistry<H: DynamicHolder<*>> {

    open val actions = hashMapOf<String, InventoryAction<H>>()

    open fun register(vararg actions: InventoryAction<H>): ActionRegistry<H> {
        actions.forEach { register(action = it) }
        return this
    }

    open fun register(action: InventoryAction<H>): ActionRegistry<H> {
        actions[action.id] = action
        return this
    }

    fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
        val id = item.action?.split(' ', limit = 2)?.first() ?: error("InventoryItem action is null")
        val action = actions[id.lowercase()] ?: return item.item.item.also {
            plugin.warn("Unknown action ${item.action}")
        }
        return action.parseAction(slot, item, holder)
    }

    companion object {

        fun <H: DynamicHolder<*>> base(): ActionRegistry<H> {
            return ActionRegistry<H>().register(CommonActions.close(), CommonActions.command())
        }
    }

}