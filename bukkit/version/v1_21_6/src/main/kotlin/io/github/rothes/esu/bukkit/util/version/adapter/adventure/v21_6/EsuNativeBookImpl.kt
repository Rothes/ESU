package io.github.rothes.esu.bukkit.util.version.adapter.adventure.v21_6

import com.mojang.serialization.JsonOps
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuNativeBook
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ContainerStateIDGetter
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ComponentSerialization
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

    private val STATE_ID_GETTER by Versioned(ContainerStateIDGetter::class.java)
    private val OPS = JsonOps.INSTANCE

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
        val decode = ComponentSerialization.CODEC.decode(OPS, GsonComponentSerializer.gson().serializeToTree(message))
        val pair = decode.getOrThrow { RuntimeException(it) }
        return pair.first
    }

    private fun <T: Any> T.toFilterable() = Filterable.passThrough(this)

}