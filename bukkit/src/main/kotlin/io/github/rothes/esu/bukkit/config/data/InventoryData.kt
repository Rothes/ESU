package io.github.rothes.esu.bukkit.config.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.lib.configurate.objectmapping.meta.NodeKey
import io.github.rothes.esu.lib.configurate.objectmapping.meta.Setting
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

open class InventoryData<T>(
    val inventoryType: InventoryType? = null,
    val size: Int? = null,
    val title: String = "Menu",
    val layout: String = """
        .........
        .........
        .........
        .........
        .........
        .........
    """.trimIndent(),
    val icons: Map<Char, InventoryItem> = linkedMapOf('.' to InventoryItem()),
): ConfigurationPart {

    @Setting("type-icons")
    private var typeIconsInternal: T? = null
    val typeIcons: T
        get() = typeIconsInternal ?: error("TypeIcons obj is not set!")

    constructor(
        inventoryType: InventoryType? = null,
        size: Int? = null,
        title: String = "Menu",
        layout: String,
        icons: Map<Char, InventoryItem> = linkedMapOf(' ' to InventoryItem()),
        typeIcons: T
    ): this(inventoryType, size, title, layout, icons) {
        this.typeIconsInternal = typeIcons
    }

    data class InventoryItem(
        val item: ItemData = ItemData(Material.AIR),
        val type: String? = null,
        @NoDeserializeNull
        val actions: List<String>? = null,
        @field:NodeKey
        val key: Char = '\u0000',
    ): ConfigurationPart

}
