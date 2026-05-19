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

package io.github.rothes.esu.bukkit.util.version.adapter.moonrise.v21

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import org.bukkit.entity.Player

object ChunkLimiterHandlerImpl : ChunkLimiterHandler() {

    private const val MAX_RATE: Long = 10000

    private fun previewAllocation(player: Player, type: Type, take: Long, time: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        return limiter.previewAllocation(time, getGlobalMaxRate(type), take)
    }

    override fun getAllocationLastSecond(player: Player, type: Type): Long {
        return getGlobalMaxRate(type).toLong() - previewAllocation(player, type, MAX_RATE)
    }

    override fun previewAllocation(player: Player, type: Type, take: Long): Long {
        return previewAllocation(player, type, take, System.nanoTime())
    }

    override fun takeAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        return limiter.takeAllocation(System.nanoTime(), take.toDouble(), MAX_RATE)
    }

}