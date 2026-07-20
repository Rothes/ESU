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

package io.github.rothes.esu.bukkit.util.inventory

import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import org.bukkit.craftbukkit.inventory.CraftInventoryCustom
import org.bukkit.inventory.Inventory

object InventoryUtils {

    @JvmStatic
    val Inventory.isEsuInventory: Boolean
        get() = this is CraftInventoryCustom && holder is EsuInvHolder<*>

    @JvmStatic
    val Inventory.esuHolder: EsuInvHolder<*>?
        get() = if (this is CraftInventoryCustom) holder as? EsuInvHolder<*> else null

}
