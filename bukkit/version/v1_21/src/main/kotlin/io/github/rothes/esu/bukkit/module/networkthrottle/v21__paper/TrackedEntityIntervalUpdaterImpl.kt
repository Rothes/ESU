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

package io.github.rothes.esu.bukkit.module.networkthrottle.v21__paper

import io.github.rothes.esu.bukkit.module.networkthrottle.EntityUpdateInterval
import io.github.rothes.esu.core.util.UnsafeUtils.usIntAccessor
import net.minecraft.server.level.ServerEntity
import net.minecraft.world.entity.Entity

object TrackedEntityIntervalUpdaterImpl : EntityUpdateInterval.TrackedEntityIntervalUpdater() {

    val SERVER_ENTITY_UPDATE_INTERVAL = ServerEntity::class.java.getDeclaredField("updateInterval").usIntAccessor

    override fun handleEntity(entity: Entity): Boolean {
        val tracker = entity.`moonrise$getTrackedEntity`() ?: return false
        val se = tracker.serverEntity
        SERVER_ENTITY_UPDATE_INTERVAL[se] = EntityUpdateInterval.INSTANCE[entity.type]
        return true
    }

}