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

package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.module.configuration.EnableTogglable
import org.bukkit.Bukkit

object DynamicChunkSendRate: CommonFeature<DynamicChunkSendRate.FeatureConfig, EmptyConfiguration>() {

    private const val CHANNEL_ID = "esu:dynamic_chunk_send_rate_limit"


    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (!ServerCompatibility.isProxyMode) return errFail { "This server is not on BungeeCord mode or Velocity mode".message }
            if (!ChunkLimiterHandler.isSupported) return errFail { "Server not supported".message }
            null
        }
    }

    override fun onEnable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_ID) { _, player, _ ->

            var times = 10
            fun func() {
                Scheduler.schedule(player, 1) {
                    if (--times > 0) {
                        val handler = ChunkLimiterHandler.instance
                        for (type in ChunkLimiterHandler.Type.entries) {
                            val allocationUnused = handler.previewAllocation(player, type, Long.MAX_VALUE)
                            val toTake = allocationUnused - 24
                            if (toTake < 0) continue
                            handler.takeAllocation(player, type, toTake)
                        }
                        func()
                    }
                }
            }
            func()
        }
    }

    override fun onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_ID)
    }

    @Comment("Enable DynamicChunkSendRate. Make sure you have velocity mode on, and installed ESU on velocity.")
    data class FeatureConfig(
        override val enabled: Boolean = ServerCompatibility.isProxyMode,
    ): EnableTogglable, ConfigurationPart

}