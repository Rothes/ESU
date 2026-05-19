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

package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

interface TickThreadAdapter {

    fun isOwnedByCurrentRegion(entity: Entity): Boolean = Bukkit.isPrimaryThread()
    fun isOwnedByCurrentRegion(location: Location): Boolean = Bukkit.isPrimaryThread()

    companion object {

        val instance = if (ServerCompatibility.isFolia) Folia else CB

        fun Entity.checkTickThread(): Boolean = instance.isOwnedByCurrentRegion(this)
        fun Location.checkTickThread(): Boolean = instance.isOwnedByCurrentRegion(this)

    }

    private object CB: TickThreadAdapter

    private object Folia: TickThreadAdapter {

        override fun isOwnedByCurrentRegion(entity: Entity): Boolean {
            return Bukkit.isOwnedByCurrentRegion(entity)
        }

        override fun isOwnedByCurrentRegion(location: Location): Boolean {
            return Bukkit.isOwnedByCurrentRegion(location)
        }

    }

}