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

package io.github.rothes.esu.bukkit.util.version.adapter.v21

import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView

class InventoryAdapterImpl: InventoryAdapter {

    override fun getTopInventory(inventoryView: InventoryView): Inventory {
        return inventoryView.topInventory
    }

}