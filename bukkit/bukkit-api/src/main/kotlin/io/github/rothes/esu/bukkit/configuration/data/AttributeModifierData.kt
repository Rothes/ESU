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

import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import java.util.*

data class AttributeModifierData(
    val key: String = UUID.randomUUID().toString().lowercase(),
    val amount: Double = 0.0,
    val operation: AttributeModifier.Operation? = null,
    val slot: String? = null,
) {

    val namespacedKey by lazy {
        NamespacedKey.fromString(key) ?: error("Invalid name $key")
    }

    val slotGroup: EquipmentSlotGroup by lazy {
        slot?.let { EquipmentSlotGroup.getByName(it) ?: error("Unknown slot $slot") }
            ?: EquipmentSlotGroup.ANY
    }

    val bukkit by lazy {
        // Confirmed on 1.21.1, not sure which version since added
        AttributeModifier(namespacedKey, amount, operation ?: AttributeModifier.Operation.ADD_NUMBER, slotGroup)
    }

}