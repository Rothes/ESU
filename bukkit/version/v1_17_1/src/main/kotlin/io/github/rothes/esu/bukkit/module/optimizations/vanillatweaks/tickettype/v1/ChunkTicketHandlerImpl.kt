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

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.v1

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeCommandsFeature
import io.github.rothes.esu.bukkit.util.CoordinateUtils
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import net.minecraft.server.level.DistanceManager
import net.minecraft.server.level.ServerChunkCache
import org.bukkit.World
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld

object ChunkTicketHandlerImpl: TicketTypeCommandsFeature.ChunkTicketHandler {

    private val ACCESSOR = ServerChunkCache::class.java.getDeclaredField("distanceManager").getter
    private val INVOKER = DistanceManager::class.java.getDeclaredMethod("getTicketDebugString", Long::class.java).handle

    override fun sendTicketDebugString(user: User, chunkX: Int, chunkZ: Int, world: World) {
        val chunkSource = (world as CraftWorld).handle.getChunkSource() // This field is private on Spigot, call method getter
        val distanceManager = ACCESSOR.invokeExact(chunkSource) as DistanceManager
        val chunkKey = CoordinateUtils.getChunkKey(chunkX, chunkZ)
        user.message(INVOKER.invokeExact(distanceManager, chunkKey) as String)
    }

}