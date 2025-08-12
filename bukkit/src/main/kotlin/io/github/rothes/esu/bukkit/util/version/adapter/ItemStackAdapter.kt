package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.legacy
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.util.ComponentUtils.esu
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.core.util.ComponentUtils.server
import io.github.rothes.esu.lib.net.kyori.adventure.text.Component
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

        fun <T: ItemMeta, R> editMeta(itemStack: ItemStack, block: (T) -> R): R? {
            val meta = itemStack.itemMeta ?: return null
            @Suppress("UNCHECKED_CAST")
            val ret = block(meta as T)
            itemStack.itemMeta = meta
            return ret
        }

        @JvmName("editMetaGetKt")
        fun <R: Any> ItemStack.metaGet(block: (meta: ItemMeta) -> R): R {
            return editMeta(this, block) ?: throw IllegalArgumentException("ItemStack does not have a meta")
        }
        fun <T: ItemMeta, R: Any> ItemStack.metaGet(block: (meta: T) -> R): R {
            return editMeta(this, block) ?: throw IllegalArgumentException("ItemStack does not have a meta")
        }

        @JvmName("editMetaKt")
        fun ItemStack.meta(block: (meta: ItemMeta) -> Unit): Boolean {
            return editMeta(this, block) != null
        }
        fun <T: ItemMeta> ItemStack.meta(block: (meta: T) -> Unit): Boolean {
            return editMeta(this, block) != null
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