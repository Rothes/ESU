/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.configuration.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.lib.configurate.objectmapping.meta.NodeKey
import io.github.rothes.esu.lib.configurate.objectmapping.meta.Setting
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType

open class InventoryData<T>(
    val inventoryType: InventoryType? = null,
    val size: Int? = null,
    val title: String? = null,
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
        title: String? = null,
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
