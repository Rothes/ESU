package io.github.rothes.esu.bukkit.inventory.action

import io.github.rothes.esu.bukkit.config.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object CommonActions {

    fun <H: DynamicHolder<*>> close(): SimpleAction<H> {
        return object : SimpleAction<H>("Close") {
            override fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                holder.click(slot) { e ->
                    e.whoClicked.closeInventory()
                }
                return super.parseAction(slot, item, holder)
            }
        }
    }

    fun <H: DynamicHolder<*>> command(): SimpleAction<H> {
        return object : SimpleAction<H>("Command") {
            override fun parseAction(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                val command = item.action!!.split(' ', limit = 2).getOrNull(1)?.trimStart()
                if (command != null) {
                    holder.click(slot) { e ->
                        val player = e.whoClicked as Player
                        player.chat("/$command")
                    }
                }
                return super.parseAction(slot, item, holder)
            }
        }
    }

}