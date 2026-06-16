package io.github.rothes.esu.bukkit.util.inventory

import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom
import org.bukkit.inventory.Inventory

object InventoryUtils {

    @JvmStatic
    val Inventory.isEsuInventory: Boolean
        get() = this is CraftInventoryCustom && holder is EsuInvHolder<*>

    @JvmStatic
    val Inventory.esuHolder: EsuInvHolder<*>?
        get() {
            if (this !is CraftInventoryCustom) return null
            return holder as? EsuInvHolder<*>
        }

}
