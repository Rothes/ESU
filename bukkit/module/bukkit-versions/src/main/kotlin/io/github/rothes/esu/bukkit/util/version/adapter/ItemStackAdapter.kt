package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface ItemStackAdapter {

    fun getDisplayName(meta: ItemMeta): Component?
    fun setDisplayName(meta: ItemMeta, name: Component?)
    fun getLore(meta: ItemMeta): List<Component>?
    fun setLore(meta: ItemMeta, lore: List<Component>?)

    companion object {

        val instance = if (ServerCompatibility.isPaper) Paper else CB

        val ItemStack.empty: Boolean
            get() = isAir || amount <= 0

        val ItemStack.isAir: Boolean
            get() = type == Material.AIR

        inline fun ItemStack.ifNotEmpty(block: (ItemStack) -> Unit): ItemStack {
            if (!empty)
                block(this)
            return this
        }

        inline fun <T: ItemMeta, R> editMeta(itemStack: ItemStack, block: (T) -> R): R? {
            val meta = itemStack.itemMeta ?: return null
            @Suppress("UNCHECKED_CAST")
            val ret = block(meta as T)
            itemStack.itemMeta = meta
            return ret
        }

        inline fun <R: Any> ItemStack.metaGet(block: (meta: ItemMeta) -> R): R {
            return editMeta(this, block) ?: throw IllegalArgumentException("ItemStack does not have a meta")
        }
        @JvmName("metaGetT")
        inline fun <T: ItemMeta, R: Any> ItemStack.metaGet(block: (meta: T) -> R): R {
            return editMeta(this, block) ?: throw IllegalArgumentException("ItemStack does not have a meta")
        }

        inline fun ItemStack.meta(block: (meta: ItemMeta) -> Unit): Boolean {
            return editMeta(this, block) != null
        }
        @JvmName("metaT")
        inline fun <T: ItemMeta> ItemStack.meta(block: (meta: T) -> Unit): Boolean {
            return editMeta(this, block) != null
        }

        var ItemMeta.displayName_: Component?
            get() = instance.getDisplayName(this)
            set(value) = instance.setDisplayName(this, value)
        var ItemMeta.lore_: List<Component>?
            get() = instance.getLore(this)
            set(value) = instance.setLore(this, value)

    }


    @Suppress("DEPRECATION")
    private object CB: ItemStackAdapter {

        override fun getDisplayName(meta: ItemMeta): Component? {
            return if (meta.hasDisplayName()) meta.displayName.legacy else null
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
            return meta.displayName()?.esu
        }
        override fun setDisplayName(meta: ItemMeta, name: Component?) {
            meta.displayName(name?.server)
        }
        override fun getLore(meta: ItemMeta): List<Component>? {
            return meta.lore()?.map { it.esu }
        }
        override fun setLore(meta: ItemMeta, lore: List<Component>?) {
            meta.lore(lore?.map { it.server })
        }
    }

}