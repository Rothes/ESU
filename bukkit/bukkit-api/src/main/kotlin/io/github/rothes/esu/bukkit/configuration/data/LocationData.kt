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

package io.github.rothes.esu.bukkit.configuration.data

import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.NoDeserializeIf
import org.bukkit.Bukkit
import org.bukkit.Location

data class LocationData(
    val world: String? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    @NoDeserializeIf("0.0")
    val yaw: Float = 0.0f,
    @NoDeserializeIf("0.0")
    val pitch: Float = 0.0f,
): ConfigurationPart {

    val bukkit: Location
        get() = Location(world?.let { Bukkit.getWorld(it) }, x, y, z, yaw, pitch)

    operator fun plus(other: LocationData): LocationData {
        return LocationData(world, x + other.x, y + other.y, z + other.z, yaw, pitch)
    }

    operator fun minus(other: LocationData): LocationData {
        return LocationData(world, x - other.x, y - other.y, z - other.z, yaw, pitch)
    }

}
