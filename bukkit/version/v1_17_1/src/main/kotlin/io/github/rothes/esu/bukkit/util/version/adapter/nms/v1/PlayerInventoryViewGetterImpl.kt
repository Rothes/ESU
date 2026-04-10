package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.PlayerInventoryViewGetter
import net.minecraft.world.inventory.InventoryMenu
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Player

object PlayerInventoryViewGetterImpl : PlayerInventoryViewGetter {

    override fun getInventoryMenu(player: Player): InventoryMenu {
        val nms = (player as CraftPlayer).handle
        return nms.inventoryMenu
    }

}