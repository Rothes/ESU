package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.legacy
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface ItemStackAdapter {

    fun getDisplayName(meta: ItemMeta): Component?
    fun setDisplayName(meta: ItemMeta, name: Component?)
    fun getLore(meta: ItemMeta): List<Component>?
    fun setLore(meta: ItemMeta, lore: List<Component>?)

    companion object {

        val instance = if (ServerCompatibility.paper) Paper else CB

        val ItemStack.isAir: Boolean
            get() = type == Material.AIR

        inline fun ItemStack.ifNotAir(block: (ItemStack) -> Unit): ItemStack {
            if (!isAir)
                block(this)
            return this
        }

        fun <T: ItemMeta, R> editMeta(itemStack: ItemStack, block: (T) -> R): R {
            val meta = itemStack.itemMeta
            @Suppress("UNCHECKED_CAST")
            val ret = block(meta as T)
            itemStack.itemMeta = meta
            return ret
        }

        @JvmName("editMetaKt")
        fun <R> ItemStack.meta(block: (meta: ItemMeta) -> R): R {
            return editMeta(this, block)
        }
        fun <T: ItemMeta, R> ItemStack.meta(block: (meta: T) -> R): R {
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