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

package io.github.rothes.esu.bukkit.util.version.adapter.nms

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Entity as BukkitEntity
import org.bukkit.entity.Player as BukkitPlayer

interface EntityHandleGetter {

    /**
     * Get Nms handle without folia region thread check
     *
     * */
    fun getHandle(entity: BukkitEntity): Entity

    companion object {

        val INSTANCE = versioned<EntityHandleGetter>()

        val BukkitEntity.handle: Entity
            get() = INSTANCE.getHandle(this)
        val BukkitPlayer.handle: ServerPlayer
            get() {
                // Cannot add this function to interface, KClassImpl.getObjectInstance will fail
                this as CraftPlayer
                return this.handle
            }

    }

}