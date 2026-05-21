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

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration

object ChunkDataThrottle: CommonFeature<ChunkDataThrottle.FeatureConfig, EmptyConfiguration>() {

    init {
        if (ServerCompatibility.serverVersion >= 18) registerFeature(ChunkDataThrottleHandler)
    }

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents() ?: let {
            if (ServerCompatibility.serverVersion < 18) {
                core.err("[ChunkDataThrottle] This feature requires Minecraft 1.18+")
                return Feature.AvailableCheck.fail { "This feature requires Minecraft 1.18+".message }
            }
            null
        }
    }

    override fun onEnable() {
    }

    @Comment("""
        Helps to reduce chunk upload bandwidth. Plugin will compress invisible blocks in chunk data packet.
        If necessary, we send a full chunk data again.
        This can save about 50% bandwidth usage in overworld and 30% in nether averagely.
        Make sure you have enabled network-compression on proxy or this server.
    """)
    class FeatureConfig: BaseFeatureConfiguration(true)

}