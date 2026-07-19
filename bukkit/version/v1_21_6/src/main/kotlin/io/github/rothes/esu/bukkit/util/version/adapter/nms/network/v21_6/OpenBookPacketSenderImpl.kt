package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v21_6

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.BundlePacketSender
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.OpenBookPacketSender
import io.github.rothes.esu.core.util.ComponentUtils.plainText
import io.github.rothes.esu.lib.adventure.inventory.Book
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket
import net.minecraft.server.network.Filterable
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WrittenBookContent
import org.bukkit.entity.Player

object OpenBookPacketSenderImpl: OpenBookPacketSender {

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

        val handle = player.handle
        BundlePacketSender.INSTANCE.send(
            player,
            listOf(
                ClientboundSetPlayerInventoryPacket(handle.inventory.selectedSlot, item),
                ClientboundOpenBookPacket(InteractionHand.MAIN_HAND),
                ClientboundSetPlayerInventoryPacket(handle.inventory.selectedSlot, handle.inventory.selectedItem)
            )
        )
    }

    private fun <T: Any> T.toFilterable() = Filterable.passThrough(this)

}