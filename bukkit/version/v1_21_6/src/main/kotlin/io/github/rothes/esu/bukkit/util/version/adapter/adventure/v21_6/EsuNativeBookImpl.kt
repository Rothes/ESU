/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util.version.adapter.adventure.v21_6

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuNativeBook
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ComponentSerializer
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ContainerStateIDGetter
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket
import net.minecraft.server.network.Filterable
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WrittenBookContent
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import net.minecraft.network.chat.Component as MinecraftComponent

@Suppress("UnstableApiUsage")
object EsuNativeBookImpl: EsuNativeBook<MinecraftComponent> {

    private val STATE_ID_GETTER = versioned<ContainerStateIDGetter>()
    private val SERIALIZER = versioned<ComponentSerializer>()

    override fun createBook(title: String, author: String, pages: Iterable<MinecraftComponent>): ItemStack {
        val item = ItemStack(Items.WRITTEN_BOOK)
        item.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            WrittenBookContent(title.toFilterable(), author, 0, pages.map { it.toFilterable() }, true)
        )
        return item
    }

    override fun openBook(viewer: Player, book: ItemStack) {
        viewer as CraftPlayer
        val player = viewer.handle
        val connection = player.connection

        fun sendOffHand(item: ItemStack) {
            connection.send(
                ClientboundContainerSetSlotPacket(0, STATE_ID_GETTER[player], InventoryMenu.SHIELD_SLOT, item)
            )
        }

        try {
            sendOffHand(book)
            connection.send(ClientboundOpenBookPacket(InteractionHand.OFF_HAND))
        } finally {
            sendOffHand(CraftItemStack.asNMSCopy(viewer.inventory.itemInOffHand))
        }
    }

    override fun createMessage(viewer: Player, message: Component): MinecraftComponent {
        return SERIALIZER.toMinecraft(message)
    }

    private fun <T: Any> T.toFilterable() = Filterable.passThrough(this)

}