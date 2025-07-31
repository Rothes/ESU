package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.legacy
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface ItemStackAdapter {

    fun getDisplayName(meta: ItemMeta): Component?
    fun setDisplayName(meta: ItemMeta, name: Component?)
    fun getLore(meta: ItemMeta): List<Component>?
    fun setLore(meta: ItemMeta, lore: List<Component>?)

    companion object {

        val instance = if (ServerCompatibility.paper) Paper else CB

        fun <T: ItemMeta> editMeta(itemStack: ItemStack, block: (T) -> Unit) {
            val meta = itemStack.itemMeta
            @Suppress("UNCHECKED_CAST")
            block(meta as T)
            itemStack.itemMeta = meta
        }

        @JvmName("editMetaKt")
        fun ItemStack.meta(block: (meta: ItemMeta) -> Unit) {
            return editMeta(this, block)
        }
        fun <T: ItemMeta> ItemStack.meta(block: (meta: T) -> Unit) {
            return editMeta(this, block)
        }

        var ItemMeta.displayNameV: Component?
            get() = instance.getDisplayName(this)
            set(value) = instance.setDisplayName(this, value)
        var ItemMeta.loreV: List<Component>?
            get() = instance.getLore(this)
            set(value) = instance.setLore(this, value)

    }


    @Suppress("DEPRECATION")
    private object CB: ItemStackAdapter {

        override fun getDisplayName(meta: ItemMeta): Component? {
            return meta.displayName.legacy
        }
        override fun setDisplayName(meta: ItemMeta, name: Component?) {
            meta.setDisplayName(name?.legacy)
        }
        override fun getLore(meta: ItemMeta): List<Component>? {
            return meta.lore?.map { it.legacy }
        }
        override fun setLore(meta: ItemMeta, lore: List<Component>?) {
            meta.lore = lore?.map { it.legacy }
        }

    }

    private object Paper: ItemStackAdapter {

        override fun getDisplayName(meta: ItemMeta): Component? {
            return meta.displayName()
        }
        override fun setDisplayName(meta: ItemMeta, name: Component?) {
            meta.displayName(name)
        }
        override fun getLore(meta: ItemMeta): List<Component>? {
            return meta.lore()
        }
        override fun setLore(meta: ItemMeta, lore: List<Component>?) {
            meta.lore(lore)
        }
    }

}