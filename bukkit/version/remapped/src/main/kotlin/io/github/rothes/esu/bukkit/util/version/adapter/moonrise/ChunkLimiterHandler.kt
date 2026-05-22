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

package io.github.rothes.esu.bukkit.util.version.adapter.moonrise

import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.util.ReflectionUtils.get
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import io.github.rothes.esu.core.util.extension.ClassUtils
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import kotlin.math.max

abstract class ChunkLimiterHandler {

    private val gen = RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java
        .getDeclaredField("chunkGenerateTicketLimiter").getter
    private val load = RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java
        .getDeclaredField("chunkLoadTicketLimiter").getter
    private val send = RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java
        .getDeclaredField("chunkSendLimiter").getter

    abstract fun getAllocationLastSecond(player: Player, type: Type): Long
    abstract fun previewAllocation(player: Player, type: Type, take: Long): Long
    abstract fun takeAllocation(player: Player, type: Type, take: Long): Long

    open fun getGlobalMaxRate(type: Type): Double {
        val config = GlobalConfiguration.get().chunkLoadingBasic
        val configRate = when (type) {
            Type.LOAD       -> config.playerMaxChunkLoadRate
            Type.GENERATE   -> config.playerMaxChunkGenerateRate
            Type.SEND       -> config.playerMaxChunkSendRate
        }
        return if (0 < configRate && configRate <= 10000.0) max(1.0, configRate) else 10000.0
    }

    fun isMoonriseChunkLoaderSet(player: Player): Boolean {
        // This may return null, when player just joined the server
        @Suppress("SENSELESS_COMPARISON")
        return player.moonriseChunkLoader != null
    }

    protected val Player.moonriseChunkLoader: RegionizedPlayerChunkLoader.PlayerChunkLoaderData
        get() = (this as CraftPlayer).handle.`moonrise$getChunkLoader`()

    protected fun RegionizedPlayerChunkLoader.PlayerChunkLoaderData.getLimiter(type: Type): Any {
        return when (type) {
            Type.GENERATE   -> gen[this]
            Type.LOAD       -> load[this]
            Type.SEND       -> send[this]
        }
    }

    enum class Type {
        GENERATE,
        LOAD,
        SEND,
    }

    companion object {

        val isSupported
            get() = ClassUtils.existsClass($$"ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader$PlayerChunkLoaderData")

        val instance by lazy { ChunkLimiterHandler::class.java.versioned() }
    }

}
