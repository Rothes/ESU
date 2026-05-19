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

package io.github.rothes.esu.bukkit.inventory.type

import io.github.rothes.esu.bukkit.configuration.data.InventoryData
import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.inventory.ItemStack

open class TypeRegistry<H: DynamicHolder<*>> {

    open val registry = hashMapOf<String, InventoryType<H>>()

    open fun register(vararg types: InventoryType<H>): TypeRegistry<H> {
        types.forEach { register(type = it) }
        return this
    }

    open fun register(type: InventoryType<H>): TypeRegistry<H> {
        registry[type.name.lowercase()] = type
        return this
    }

    fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
        val idRaw = item.type?.lowercase() ?: error("InventoryItem type is null")
        if (idRaw.isEmpty()) error("InventoryItem type is empty")
        val id = if (idRaw.first() == '[') {
            idRaw.substringBefore(']').substring(1)
        } else idRaw
        val type = registry[id] ?: return item.item.item.also {
            core.warn("Unknown type '${item.type}'")
        }
        return type.parseType(slot, item, holder)
    }

}