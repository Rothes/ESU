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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v20_5

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ContainerStateIDGetter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.BundlePacketSender
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.OpenBookPacketSender
import io.github.rothes.esu.core.util.ComponentUtils.plainText
import io.github.rothes.esu.lib.adventure.inventory.Book
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket
import net.minecraft.server.network.Filterable
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WrittenBookContent
import org.bukkit.entity.Player

object OpenBookPacketSenderImpl: OpenBookPacketSender {

    private val STATE_ID = versioned<ContainerStateIDGetter>()

    override fun openBook(player: Player, book: Book) {
        val item = ItemStack(Items.WRITTEN_BOOK)
        item.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            WrittenBookContent(
                book.title().plainText.toFilterable(),
                book.author().plainText,
                0,
                book.pages().map { it.toMinecraft().toFilterable() },
                true
            )
        )

        val inventory = player.handle.inventory
        val slot = inventory.items.size + inventory.selected
        BundlePacketSender.INSTANCE.send(
            player,
            listOf(
                ClientboundContainerSetSlotPacket(0, STATE_ID[player], slot, item),
                ClientboundOpenBookPacket(InteractionHand.MAIN_HAND),
                ClientboundContainerSetSlotPacket(0, STATE_ID[player], slot, inventory.getSelected())
            )
        )
    }

    private fun <T: Any> T.toFilterable() = Filterable.passThrough(this)

}