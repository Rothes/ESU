package io.github.rothes.esu.bukkit.util.version.adapter.adventure.v1

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.EsuNativeBook
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.world.item.ItemStack
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
object EsuNativeBookImpl: EsuNativeBook<Any> {

    override fun isSupported(): Boolean {
        return false
    }

    override fun createBook(title: String, author: String, pages: Iterable<Any?>): ItemStack? {
        TODO("Not yet implemented")
    }

    override fun openBook(viewer: Player, book: ItemStack) {
        TODO("Not yet implemented")
    }

    override fun createMessage(viewer: Player, message: Component): Any? {
        TODO("Not yet implemented")
    }
}