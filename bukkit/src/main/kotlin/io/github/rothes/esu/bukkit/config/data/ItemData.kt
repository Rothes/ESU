package io.github.rothes.esu.bukkit.config.data

import com.destroystokyo.paper.profile.ProfileProperty
import dev.lone.itemsadder.api.CustomStack
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.displayName_
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.lore_
import io.github.rothes.esu.bukkit.util.version.adapter.ItemStackAdapter.Companion.meta
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeIf
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ComponentUtils
import io.github.rothes.esu.core.util.ComponentUtils.nonItalic
import io.github.rothes.esu.lib.adventure.text.Component
import io.github.rothes.esu.lib.adventure.text.minimessage.MiniMessage
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver
import io.lumine.mythic.bukkit.BukkitAdapter
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.drops.DropMetadataImpl
import net.Indyuce.mmoitems.MMOItems
import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

data class ItemData(
    val material: Material? = null,
    val itemsAdderId: String? = null,
    val mythicMobsItemId: String? = null,
    val mmoItemsItemType: String? = null,
    val mmoItemsItemId: String? = null,
    val craftEngineItemId: String? = null,

    val displayName: String? = null,
    @NoDeserializeNull
    val lore: List<String>? = null,
    @NoDeserializeNull
    val enchantments: Map<String, Int>? = null,
    @NoDeserializeNull
    val attributes: Map<Attribute, List<AttributeModifierData>>? = null,
    @NoDeserializeNull
    val itemFlags: List<ItemFlag>? = null,
    @NoDeserializeIf("1")
    val amount: Int = 1,
    val playerTexture: String? = null,
    val customModelData: Int? = null,
    val itemModel: String? = null,
    val tooltipStyle: String? = null,
): ConfigurationPart {

    val displayNameComponent: Component? by lazy { displayName?.let(ComponentUtils::fromMiniMessage) }
    val loreComponent: List<Component>? by lazy { lore?.map(ComponentUtils::fromMiniMessage) }
    val tooltipStyleObj by lazy { tooltipStyle?.let(NamespacedKey::fromString) }
    val create: ItemStack
        get() = createItemByType().also { item ->
            item.meta { meta ->
                customModelData?.let { meta.setCustomModelData(customModelData) }
                itemModel?.let { meta.itemModel = NamespacedKey.fromString(itemModel) }
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
                attributes?.let {
                    for ((attr, mods) in attributes) {
                        for (mod in mods) {
                            meta.addAttributeModifier(attr, mod.bukkit)
                        }
                    }
                }
                itemFlags?.let {
                    meta.addItemFlags(*it.toTypedArray())
                }
                playerTexture?.let {
                    if (meta !is SkullMeta) return@let
                    meta.playerProfile = Bukkit.createProfile(UUID.randomUUID()).apply {
                        setProperty(ProfileProperty("textures", playerTexture))
                    }
                }
                tooltipStyleObj?.let { meta.tooltipStyle = it }
            }
        }
    val itemUnsafe: ItemStack by lazy { parsed(ConsoleUser) }
    val item
        get() = itemUnsafe.clone()

    fun parsed(user: User, vararg params: TagResolver): ItemStack {
        val item = create

        item.meta { meta ->
            displayName?.let {
                meta.displayName_ = user.buildMiniMessage(it, params = params).nonItalic
            }
            lore?.let { lore ->
                val built = lore.map { user.buildMiniMessage(it, params = params) }
                val list = arrayListOf<Component>()
                built.forEach { component ->
                    val serialize = MiniMessage.miniMessage().serialize(component)
                    if (serialize.contains('\n') || serialize.contains("<br>")) {
                        list.addAll(
                            serialize.split("<br>", "\n")
                                .map { MiniMessage.miniMessage().deserialize(it).nonItalic }
                        )
                    } else {
                        list.add(component.nonItalic)
                    }
                }
                meta.lore_ = list
            }
        }
        return item
    }

    fun matches(itemStack: ItemStack): Boolean {
        if (material == null && itemsAdderId == null && mythicMobsItemId == null && itemStack.type != Material.AIR) {
            return false
        }
        craftEngineItemId?.let {
            val key = CraftEngineItems.getCustomItemId(itemStack) ?: return false
            return if (key.toString() != craftEngineItemId) false else checkProp(itemStack)
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
        craftEngineItemId?.let { ce ->
            CraftEngineItems.byId(Key.of(ce))?.buildItemStack() ?: error("CraftEngine item '$ce' does not exist")
        } ?: itemsAdderId?.let { ia ->
            CustomStack.getInstance(ia)!!.itemStack.also { it.amount = amount }
        } ?: mythicMobsItemId?.let {
            BukkitAdapter.adapt(
                MythicBukkit.inst().itemManager.getItem(mythicMobsItemId)
                    .orElseThrow { IllegalStateException("MM Item \"$mythicMobsItemId\" does not exist") }
                    .generateItemStack(DropMetadataImpl(null, null), amount))
        } ?: mmoItemsItemId?.let {
            MMOItems.plugin.getItem(mmoItemsItemType, mmoItemsItemId)?.also { it.amount = amount }
                ?: error("MMOItem \"$mmoItemsItemType:$mythicMobsItemId\" does not exist")
        } ?: ItemStack(material ?: Material.AIR, amount)

    private fun checkProp(itemStack: ItemStack): Boolean {
        val meta = itemStack.itemMeta
        displayNameComponent?.let {
            if (meta.displayName_ != it) return false
        }
        loreComponent?.let {
            if (meta.lore_ != it) return false
        }
        tooltipStyleObj?.let {
            if (meta.tooltipStyle != it) return false
        }
        return true
    }

    // Legacy APIs

    @Deprecated("Since 0.9.0, legacy api support only")
    constructor(material: Material? = null, itemsAdderId: String? = null, mythicMobsItemId: String? = null,
                mmoItemsItemType: String? = null, mmoItemsItemId: String? = null, craftEngineItemId: String? = null,
                displayName: String? = null, lore: List<String>? = null, enchantments: Map<String, Int>? = null,
                amount: Int = 1, playerTexture: String? = null, customModelData: Int? = null, itemModel: String? = null,
                tooltipStyle: String? = null,
    ): this(material, itemsAdderId, mythicMobsItemId, mmoItemsItemType, mmoItemsItemId, craftEngineItemId, displayName,
        lore, enchantments, null, null, amount, playerTexture, customModelData, itemModel,
        tooltipStyle)

}