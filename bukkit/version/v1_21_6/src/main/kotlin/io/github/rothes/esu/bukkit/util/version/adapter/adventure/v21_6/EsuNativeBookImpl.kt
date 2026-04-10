package io.github.rothes.esu.bukkit.util.version.adapter.adventure.v21_6

import com.mojang.serialization.JsonOps
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuNativeBook
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket
import net.minecraft.server.network.Filterable
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WrittenBookContent
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import net.minecraft.network.chat.Component as MinecraftComponent
import net.minecraft.world.item.ItemStack as MinecraftItemStack

@Suppress("UnstableApiUsage")
object EsuNativeBookImpl: EsuNativeBook<MinecraftComponent> {

    private val OPS = JsonOps.INSTANCE

    override fun createBook(title: String, author: String, pages: Iterable<MinecraftComponent>): ItemStack {
        val item = MinecraftItemStack(Items.WRITTEN_BOOK)
        item.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            WrittenBookContent(title.toFilterable(), author, 0, pages.map { it.toFilterable() }, true)
        )
        return CraftItemStack.asCraftMirror(item)
    }

    override fun openBook(viewer: Player, book: ItemStack) {
        viewer as CraftPlayer
        val player = viewer.handle
        val connection = player.connection

        fun sendOffHand(item: ItemStack) {
            val nmsItem = CraftItemStack.asNMSCopy(item)
            connection.send(
                ClientboundContainerSetSlotPacket(0, player.containerMenu.stateId, InventoryMenu.SHIELD_SLOT, nmsItem)
            )
        }

        try {
            sendOffHand(book)
            connection.send(ClientboundOpenBookPacket(InteractionHand.OFF_HAND))
        } finally {
            sendOffHand(viewer.inventory.itemInOffHand)
        }
    }

    override fun createMessage(viewer: Player, message: Component): MinecraftComponent {
        val decode = ComponentSerialization.CODEC.decode(OPS, GsonComponentSerializer.gson().serializeToTree(message))
        val pair = decode.getOrThrow { RuntimeException(it) }
        return pair.first
    }

    private fun <T: Any> T.toFilterable() = Filterable.passThrough(this)

}