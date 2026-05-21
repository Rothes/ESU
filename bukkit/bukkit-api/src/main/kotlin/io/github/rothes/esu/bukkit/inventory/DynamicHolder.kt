/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.inventory

import io.github.rothes.esu.bukkit.configuration.data.InventoryData
import io.github.rothes.esu.bukkit.inventory.action.ActionRegistry
import io.github.rothes.esu.bukkit.inventory.type.TypeRegistry
import io.github.rothes.esu.bukkit.user
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class DynamicHolder<T>(inventoryData: InventoryData<T>): EsuInvHolder<T>(inventoryData) {

    private val defers = PriorityQueue<DeferTask>(compareByDescending { it.priority })
    private val click = Int2ReferenceOpenHashMap<MutableList<(InventoryClickEvent) -> Unit>>()

    protected val typeRegistry: TypeRegistry<DynamicHolder<T>> = TypeRegistry()

    fun defer(priority: Int = 0, function: Inventory.() -> Unit): ItemStack? {
        defers.add(DeferTask(priority, function))
        return null
    }

    fun click(slot: Int, function: (InventoryClickEvent) -> Unit) {
        click.getOrPut(slot) { ArrayList() }.add(function)
    }

    override fun postInventoryBuild(inventory: Inventory) {
        defers.forEach { it.task(inventory) }
        defers.clear()
    }

    override fun handleClick(slot: Int, e: InventoryClickEvent) {
        super.handleClick(slot, e)
        click[slot]?.forEach { it(e) }
    }

    override fun parseType(slot: Int, item: InventoryData.InventoryItem): ItemStack? {
        if (item.type != null) {
            return typeRegistry.parseType(slot, item, this)
        }
        if (item.actions != null) {
            ActionRegistry.parseActions(item.actions).forEach { action ->
                click(slot) { e -> action.handle((e.whoClicked as Player).user)}
            }
        }
        return super.parseType(slot, item)
    }

    private class DeferTask(val priority: Int, val task: Inventory.() -> Unit)

}