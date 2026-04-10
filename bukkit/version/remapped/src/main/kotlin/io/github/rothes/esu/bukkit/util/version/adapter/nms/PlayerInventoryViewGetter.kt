package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.world.inventory.InventoryMenu
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

interface PlayerInventoryViewGetter {

    fun getInventoryMenu(player: Player): InventoryMenu

    fun getCraftingInventoryView(player: Player): InventoryView {
        return getInventoryMenu(player).bukkitView
    }

}