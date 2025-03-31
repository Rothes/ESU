package io.github.rothes.esu.bukkit.config.data

import dev.lone.itemsadder.api.CustomStack
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeIf
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.core.util.ComponentUtils
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.drops.DropMetadataImpl
import net.Indyuce.mmoitems.MMOItems
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

data class ItemData(
    val material: Material? = null,
    val itemsAdderId: String? = null,
    val mythicMobsItemId: String? = null,
    val mmoItemsItemType: String? = null,
    val mmoItemsItemId: String? = null,

    val displayName: String? = null,
    @NoDeserializeNull
    val lore: List<String>? = null,
    @NoDeserializeNull
    val enchantments: Map<String, Int>? = null,
    @NoDeserializeIf("1")
    val amount: Int = 1,
    val customModelData: Int? = null,
): ConfigurationPart {

    val displayNameComponent: Component? by lazy { displayName?.let(ComponentUtils::fromMiniMessage) }
    val loreComponent: List<Component>? by lazy { lore?.map(ComponentUtils::fromMiniMessage) }
    val create: ItemStack
        get() = createItemByType().also {
            it.editMeta { meta ->
                customModelData?.let { meta.setCustomModelData(customModelData) }
                enchantments?.let {
                    for ((key, level) in enchantments) {
                        val enchantment = Enchantment.getByKey(NamespacedKey.fromString(key))
                        if (enchantment == null) {
                            plugin.err("Unknown enchantment $key")
                            continue
                        }
                        meta.addEnchant(enchantment, level, true)
                    }
                }
            }
        }
    val itemUnsafe: ItemStack by lazy {
        create.also { item ->
            item.editMeta { meta ->
                displayNameComponent?.let { meta.displayName(it) }
                loreComponent?.let { meta.lore(it) }
            }
        }
    }
    val item
        get() = itemUnsafe.clone()

    fun matches(itemStack: ItemStack): Boolean {
        if (material == null && itemsAdderId == null && mythicMobsItemId == null && itemStack.type != Material.AIR) {
            return false
        }
        itemsAdderId?.let {
            val ia = CustomStack.byItemStack(itemStack) ?: return false
            return if (ia.id != itemsAdderId) false else checkProp(itemStack)
        }
        mythicMobsItemId?.let {
            val mm =
                MythicBukkit.inst().volatileCodeHandler.itemHandler.getNBTData(itemStack).getString("MMOITEMS_ITEM_ID")
            return if (mm != mythicMobsItemId) false else checkProp(itemStack)
        }
        mmoItemsItemId?.let {
            return MMOItems.getTypeName(itemStack) == mmoItemsItemType && MMOItems.getID(itemStack) == mmoItemsItemId && checkProp(
                itemStack
            )
        }
        material?.let {
            return if (itemStack.type != material) false else checkProp(itemStack)
        }
        return true
    }

    private fun createItemByType() =
        itemsAdderId?.let { ia -> CustomStack.getInstance(ia)!!.itemStack.also { it.amount = amount } }
            ?: mythicMobsItemId?.let {
                BukkitAdapter.adapt(
                    MythicBukkit.inst().itemManager.getItem(mythicMobsItemId)
                        .orElseThrow { IllegalStateException("MM Item \"$mythicMobsItemId\" does not exist") }
                        .generateItemStack(DropMetadataImpl(null, null), amount))
            } ?: mmoItemsItemId?.let {
                MMOItems.plugin.getItem(mmoItemsItemType, mmoItemsItemId)?.also { it.amount = amount }
                    ?: error("MMOItem \"$mmoItemsItemType:$mythicMobsItemId\" does not exist")
            } ?: ItemStack(material ?: Material.AIR, amount)

    private fun checkProp(itemStack: ItemStack): Boolean {
        displayNameComponent?.let {
            if (itemStack.displayName() != it) return false
        }
        loreComponent?.let {
            if (itemStack.lore() != it) return false
        }
        return true
    }

}