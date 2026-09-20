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

package io.github.rothes.esu.bukkit.module.networkthrottle.v26_3

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import io.github.rothes.esu.core.util.ReflectionUtils.setter
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.server.level.ChunkMap
import net.minecraft.server.level.ServerEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.UpdateInterval

object TrackedEntityIntervalUpdaterImpl : EntityUpdateInterval.TrackedEntityIntervalUpdater() {

    val TRACKED_ENTITY_SERVER_ENTITY = ChunkMap.TrackedEntity::class.java.getDeclaredField("serverEntity").usObjAccessor
    val SERVER_ENTITY_UPDATE_INTERVAL = ServerEntity::class.java.getDeclaredField("updateInterval").setter

    override fun handleEntity(entity: Entity, tracker: ChunkMap.TrackedEntity) {
        val se = TRACKED_ENTITY_SERVER_ENTITY[tracker] as ServerEntity
        val entityType = entity.type
        val interval = if (entityType.hasUpdateInterval()) UpdateInterval.periodic(entityType.updateInterval()) else UpdateInterval.NEVER
        SERVER_ENTITY_UPDATE_INTERVAL.invokeExact(se, interval)
    }

}