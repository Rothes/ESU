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

package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.module.networkthrottle.*
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryValueSerializers
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.LoadedConfiguration
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import java.util.*

object NetworkThrottleModule: BukkitModule<BaseModuleConfiguration, NetworkThrottleModule.ModuleLang>() {

    init {
        registerFeature(AfkEfficiency)
        registerFeature(ChunkDataThrottle)
        registerFeature(DynamicChunkSendRate)
        registerFeature(EntityCulling)
        if (NmsRegistryValueSerializers.isSupported) {
            if (ServerCompatibility.isPaper) registerFeature(EntityTrackingRange)
            registerFeature(EntityUpdateInterval.INSTANCE)
        }
        registerFeature(HighLatencyAdjust)
        registerFeature(SkipUnnecessaryPackets)
    }

    lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override fun onEnable() {
        data = ConfigLoader.load(dataPath)
    }

    override fun onDisable() {
        super.onDisable()
        ConfigLoader.save(dataPath, data)
    }

    override fun buildConfigLoader(builder: YamlConfigurationLoader.Builder) {
        if (NmsRegistryValueSerializers.isSupported) {
            builder.defaultOptions { options ->
                options.serializers { builder ->
                    builder.register(NmsRegistryValueSerializers.instance.block)
                        .register(NmsRegistryValueSerializers.instance.entityType)
                }
            }
        }
    }

    override fun preprocessConfig(loadedConfiguration: LoadedConfiguration) {
        // v0.12.4
        val node = loadedConfiguration.node
        val from = node.node("chunk-data-throttle")
        val to = node.node("chunk-data-throttle", "chunk-handler")
        for ((key, value) in from.childrenMap()) {
            if (key != "enabled" && key != "chunk-handler") {
                to.node(value.key()).from(value)
                from.removeChild(key)
            }
        }
    }


    data class ModuleData(
        val originalViewDistance: MutableMap<UUID, Int> = linkedMapOf(),
    )

    class ModuleLang: ConfigurationPart

}


