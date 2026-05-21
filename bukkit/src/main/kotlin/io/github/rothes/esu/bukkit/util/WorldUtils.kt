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

package io.github.rothes.esu.bukkit.util

import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.util.extension.math.floorI
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

object WorldUtils {

    suspend fun findStandableSpot(column: Location, allowUnsafe: Boolean = false): Location? {
        val deferred = CompletableDeferred<Location?>()
        val location = column.clone()
        location.x = floor(location.x) + 0.5
        location.z = floor(location.z) + 0.5
        Scheduler.schedule(location) {
            val world = requireNotNull(location.world) { "Location world is null" }
            val y = if (world.environment == World.Environment.NETHER) {
                val x = floorI(location.x)
                val z = floorI(location.z)
                var y = 125
                while (true) {
                    if (y == 0) {
                        deferred.complete(if (allowUnsafe) location else null)
                        return@schedule
                    }
                    y--
                    if (world.getBlockAt(x, y + 2, z).type.isAir
                        && world.getBlockAt(x, y + 1, z).type.isAir
                        && world.getBlockAt(x, y, z).type.isSolid) {
                        break
                    }
                }
                y
            } else {
                world.getHighestBlockYAt(location)
            }
            location.y = y.toDouble() + 1
            deferred.complete(location)
        }
        return withTimeout(5.seconds) { // Some timeout for not generated chunk / low tps
            deferred.await()
        }
    }

}