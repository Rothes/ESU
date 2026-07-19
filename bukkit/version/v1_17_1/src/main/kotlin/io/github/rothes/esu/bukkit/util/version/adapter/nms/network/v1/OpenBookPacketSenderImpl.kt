package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v1

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ContainerStateIDGetter
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.BundlePacketSender
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.OpenBookPacketSender
import io.github.rothes.esu.core.util.ComponentUtils
import io.github.rothes.esu.core.util.ComponentUtils.plainText
import io.github.rothes.esu.lib.adventure.inventory.Book
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.item.WrittenBookItem
import org.bukkit.entity.Player

object OpenBookPacketSenderImpl: OpenBookPacketSender {

    private val STATE_ID = versioned<ContainerStateIDGetter>()

    override fun openBook(player: Player, book: Book) {
        val item = net.minecraft.world.item.ItemStack(Items.WRITTEN_BOOK)

        val tag = item.getOrCreateTag()
        tag.putString(WrittenBookItem.TAG_AUTHOR, book.author().plainText)
        tag.putString(WrittenBookItem.TAG_TITLE, book.title().plainText)
        val pages = ListTag()
        for (component in book.pages()) {
            pages.add(StringTag.valueOf(ComponentUtils.gsonSerializer().serialize(component)))
        }
        tag.put(WrittenBookItem.TAG_PAGES, pages)
        tag.putByte(WrittenBookItem.TAG_RESOLVED, 1)

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

}