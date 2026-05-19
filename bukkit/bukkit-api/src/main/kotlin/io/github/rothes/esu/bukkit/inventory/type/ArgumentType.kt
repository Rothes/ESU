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
import io.github.rothes.esu.bukkit.inventory.DynamicHolder
import org.bukkit.inventory.ItemStack

open class ArgumentType<H: DynamicHolder<*>>(
    final override val name: String
): InventoryType<H> {

    override fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
        return item.item.item
    }

    override fun toString(): String {
        return "ArgumentType(name='$name')"
    }

    protected val InventoryData.InventoryItem.arg
        get() = type?.substringAfter(']')?.removePrefix(" ")?.ifEmpty { null }

    companion object {
        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int,
            item: InventoryData.InventoryItem,
            arg: String?,
            holder: H,
        ) -> ItemStack?): ArgumentType<H> {
            return object : ArgumentType<H>(name) {
                override fun parseType(
                    slot: Int,
                    item: InventoryData.InventoryItem,
                    holder: H,
                ): ItemStack? {
                    return func(slot, item, item.arg, holder)
                }
            }
        }

        fun <H: DynamicHolder<*>> create(name: String, func: (
            slot: Int,
            item: InventoryData.InventoryItem,
            arg: String?,
        ) -> ItemStack?): ArgumentType<H> {
            return object : ArgumentType<H>(name) {
                override fun parseType(slot: Int, item: InventoryData.InventoryItem, holder: H): ItemStack? {
                    return func(slot, item, item.arg)
                }
            }
        }
    }

}
