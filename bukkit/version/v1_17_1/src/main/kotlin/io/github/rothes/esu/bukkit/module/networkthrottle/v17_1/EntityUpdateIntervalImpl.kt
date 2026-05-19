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

package io.github.rothes.esu.bukkit.module.networkthrottle.v17_1

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.server.level.ChunkMap
import net.minecraft.server.level.ServerEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType

object EntityUpdateIntervalImpl: EntityUpdateInterval() {

    private val ENTITY_TYPE_UPDATE_INTERVAL = EntityType::class.java.getDeclaredField("updateInterval").usIntAccessor

    override operator fun get(entityType: EntityType<*>): Int {
        return ENTITY_TYPE_UPDATE_INTERVAL[entityType]
    }

    override operator fun set(entityType: EntityType<*>, interval: Int) {
        ENTITY_TYPE_UPDATE_INTERVAL[entityType] = interval
    }

    object TrackedEntityIntervalUpdaterImpl : TrackedEntityIntervalUpdater() {

        val TRACKED_ENTITY_SERVER_ENTITY = ChunkMap.TrackedEntity::class.java.getDeclaredField("serverEntity").usObjAccessor
        val SERVER_ENTITY_UPDATE_INTERVAL = ServerEntity::class.java.getDeclaredField("updateInterval").usIntAccessor

        override fun handleEntity(entity: Entity): Boolean {
            val tracker = entity.tracker ?: return false
            val se = TRACKED_ENTITY_SERVER_ENTITY[tracker] as ServerEntity
            SERVER_ENTITY_UPDATE_INTERVAL[se] = this@EntityUpdateIntervalImpl[entity.type]
            return true
        }

    }

}