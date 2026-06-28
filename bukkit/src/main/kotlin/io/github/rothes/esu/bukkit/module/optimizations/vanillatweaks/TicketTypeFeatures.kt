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

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks

import io.github.rothes.esu.bukkit.module.OptimizationsModule
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.ChunkLoadsFeature
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.ChunkUnloadsFeature
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.ExpiryTicksFeature
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeCommandsFeature
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.LoadedConfiguration
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.util.extension.headerIfNotNull
import io.github.rothes.esu.lib.configurate.CommentedConfigurationNode

object TicketTypeFeatures: CommonFeature<TicketTypeFeatures.FeatureConfig, Unit>() {

    init {
        registerFeature(TicketTypeCommandsFeature)
        registerFeature(ExpiryTicksFeature)
        if (ServerInfo.mcVersion >= "21.7") registerFeature(ChunkLoadsFeature)
        if (ServerInfo.mcVersion >= "21.9") registerFeature(ChunkUnloadsFeature)
    }

    override val name: String = "TicketType"

    override fun onEnable() { }

    override fun configNode(base: LoadedConfiguration): LoadedConfiguration {
        val conf = ConfigLoader.loadConfiguration(
            module.configPath.parent.resolve("ticket-type.yml"),
            ConfigLoader.LoaderSettings(
                yamlLoader = {
                    (module as OptimizationsModule).buildConfigLoader(it)
                    it.defaultOptions { o ->
                        o.headerIfNotNull("""
                            Change server ticket type settings.
                            Tickets control chunk loading. For details, check https://minecraft.wiki/w/Chunk#Tickets
                            Must enable OptimizationsModule for below options to work.
                        """.trimIndent())
                    }
                },
            )
        )
        val node = base.node
        if (node.hasChild("ticket-type")) {
            /* ESU 0.15.0 schema */
            ConsoleUser.info("Migrating ticket-type configs to dedicated file", prefix = "OptimizationsModule")
            val from = node.node("ticket-type") as CommentedConfigurationNode
//            from.comment(null) // Remove comment, this appears after header // To keep user comments, we don't do it
            conf.node.mergeFrom(from)
            node.removeChild("ticket-type")
        }
        return conf
    }

    override fun postprocessConfig(configuration: LoadedConfiguration) {
        configuration.save()
    }

    class FeatureConfig: ConfigurationPart {
        @RemovedNode("0.14.1") val enabled: Boolean? = null
        @NoDeserializeNull
        @RemovedNode("0.14.1") val startupSettings: Map<String, Int>? = null
    }

}