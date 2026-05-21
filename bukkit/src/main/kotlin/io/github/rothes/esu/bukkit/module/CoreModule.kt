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

import io.github.rothes.esu.bukkit.module.core.CoreController
import io.github.rothes.esu.bukkit.module.core.DisabledProviders
import io.github.rothes.esu.bukkit.module.core.Providers
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.util.extension.headerIfNotNull
import io.github.rothes.esu.lib.configurate.yaml.YamlConfigurationLoader
import org.bukkit.Bukkit

object CoreModule: BukkitModule<CoreModule.ModuleConfig, Unit>() {

    private var _providers: Providers = DisabledProviders

    val providers: Providers get() = _providers

    override fun onEnable() {
        CoreController.onEnable()
        _providers = CoreController.RunningProviders
    }

    override fun onDisable() {
        super.onDisable()
        CoreController.onDisable()
        _providers = DisabledProviders
    }

    override fun buildConfigLoader(builder: YamlConfigurationLoader.Builder) {
        super.buildConfigLoader(builder)
        builder.defaultOptions { options ->
            options.headerIfNotNull("""
                This module will provides some important information for other modules to use,
                which doesn't create performance impact to your server.
                This is enabled by default, do not change if you don't know what you are doing.
            """.trimIndent())
        }
    }

    data class ModuleConfig(
        @Comment("""
            Enable this to store core player data to database.
            This helps modules to work more powerful.
        """)
        val persistentStorage: PersistentStorage = PersistentStorage(),
    ): BaseModuleConfiguration(true) {

        data class PersistentStorage(
            val enabled: Boolean = true,
            @Comment("""
                Set this to the name in proxy of this server.
                If you have servers which syncs player data between,
                 you can use the same server name here.
            """)
            val serverName: String = Bukkit.getServer().port.toString(),
        )
    }

}