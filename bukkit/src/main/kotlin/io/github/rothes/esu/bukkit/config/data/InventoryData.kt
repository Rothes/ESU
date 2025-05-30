package io.github.rothes.esu.bukkit.config.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.spongepowered.configurate.objectmapping.meta.NodeKey
import org.spongepowered.configurate.objectmapping.meta.Setting

open class InventoryData<T>(
    val inventoryType: InventoryType? = null,
    val size: Int? = 6 * 9,
    val title: String = "Menu",
    val layout: String = """
        .........
        .........
        .........
        .........
        .........
        .........
    """.trimIndent(),
    val icons: Map<Char, InventoryItem> = linkedMapOf(
        '.' to InventoryItem()
    ),
): ConfigurationPart {

    @Setting("fallback")
    private var fallbackInternal: T? = null
    val fallback: T
        get() = fallbackInternal ?: error("Fallback is not set!")

    constructor(
        inventoryType: InventoryType? = null,
        size: Int? = 6 * 9,
        title: String = "Menu",
        layout: String,
        icons: Map<Char, InventoryItem> = linkedMapOf(' ' to InventoryItem()),
        fallback: T
    ): this(inventoryType, size, title, layout, icons) {
        this.fallbackInternal = fallback
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
