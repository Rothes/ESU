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

package io.github.rothes.esu.bukkit.util.version.adapter

import org.bukkit.Location
import org.bukkit.entity.Entity

interface TeleportAdapter {

    fun teleport(entity: Entity, location: Location, then: ((Boolean) -> Unit)? = null)

    companion object {

        val instance = try {
            Entity::class.java.getMethod("teleportAsync", Location::class.java)
            ASync
        } catch (_: NoSuchMethodException) {
            Sync
        }

        fun Entity.tp(location: Location, then: ((Boolean) -> Unit)? = null) {
            instance.teleport(this, location, then)
        }

    }

    private object ASync : TeleportAdapter {

        override fun teleport(entity: Entity, location: Location, then: ((Boolean) -> Unit)?) {
            val future = entity.teleportAsync(location)
            if (then != null) {
                future.thenAccept(then)
            }
        }
    }

    private object Sync : TeleportAdapter {

        override fun teleport(entity: Entity, location: Location, then: ((Boolean) -> Unit)?) {
            val success = entity.teleport(location)
            if (then != null) {
                then(success)
            }
        }
    }


}