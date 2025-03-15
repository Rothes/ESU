package io.github.rothes.esu.bukkit.config.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.spongepowered.configurate.objectmapping.meta.NodeKey
import org.spongepowered.configurate.objectmapping.meta.Setting

data class InventoryData<T>(
    val inventoryType: InventoryType? = null,
    val size: Int? = 6 * 9,
    val title: String = "Menu",
    val layout: String = """
        |         
        |         
        |         
        |         
        |         
        |         
    """.trimIndent().trim(),
    val icons: Map<Char, InventoryItem> = linkedMapOf(
        ' ' to InventoryItem()
    ),
): ConfigurationPart {

    @Setting("extra")
    private var extraInternal: T? = null
    val extra: T
        get() = extraInternal ?: error("extra is not set!")

    constructor(
        inventoryType: InventoryType? = null,
        size: Int? = 6 * 9,
        title: String = "Menu",
        layout: String,
        icons: Map<Char, InventoryItem> = linkedMapOf(' ' to InventoryItem()),
        extra: T
    ): this(inventoryType, size, title, layout, icons) {
        this.extraInternal = extra
    }

    data class InventoryItem(
        val item: ItemData = ItemData(Material.AIR),
        val action: String? = null,
        @field:NodeKey
        val key: Char = '\u0000',
    ): ConfigurationPart

}
