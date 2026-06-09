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

package io.github.rothes.esu.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import io.github.rothes.esu.common.HotLoadSupport
import io.github.rothes.esu.common.command.EsuAdminCommand
import io.github.rothes.esu.common.module.AutoBroadcastModule
import io.github.rothes.esu.common.util.extension.shutdown
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.velocity.command.EsuVelocityCommandManager
import io.github.rothes.esu.velocity.config.VelocityEsuLang
import io.github.rothes.esu.velocity.module.*
import io.github.rothes.esu.velocity.user.VelocityUserManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import org.incendo.cloud.CommandManager
import org.slf4j.Logger
import java.nio.file.Path

class EsuPluginVelocity(
    val bootstrap: EsuBootstrapVelocity,
): EsuCore {

    override var initialized: Boolean = false
        private set
    override val basePermissionNode: String = "vesu"

    var enabled: Boolean = false
        private set

    var enabledHot: Boolean by InitOnce()
    var disabledHot: Boolean by InitOnce()

    val server: ProxyServer
        get() = bootstrap.server
    val logger: Logger
        get() = bootstrap.logger
    val dataDirectory: Path
        get() = bootstrap.dataDirectory
    val container: PluginContainer
        get() = bootstrap.container

    init {
        EsuCore.instance = this
        enabledHot = byServerUtils()
    }

    override val commandManager: CommandManager<User> by lazy { EsuVelocityCommandManager() }

    fun onProxyInitialization() {
        EsuConfig           // Load global config
        VelocityEsuLang     // Load global locale
        StorageManager      // Load database
        ColorSchemes        // Load color schemes
        UpdateCheckerMan    // Init update checker
        VelocityUserManager // Init user manager
        server.allPlayers.forEach { it.user }

        ServerHotLoadSupport(enabledHot).onEnable()

        ModuleManager.addModule(AutoBroadcastModule)
        ModuleManager.addModule(AutoRestartModule)
        ModuleManager.addModule(NetworkThrottleModule)
        ModuleManager.addModule(ServerInfoModule)
        ModuleManager.addModule(UserNameVerifyModule)
        ModuleManager.addModule(AutoReloadExtensionPluginsModule)

        // Register commands
        EsuAdminCommand.register("vesu") {
            VelocityEsuLang.reloadConfig()
            UpdateCheckerMan.reload()
        }

        server.allPlayers.forEach { it.user }

        bootstrap.metricsFactory.make(bootstrap, 24826) // bStats

        initialized = true
        enabled = true
    }

    @Subscribe
    fun onDisable(e: ProxyShutdownEvent) {
        enabled = false
        disabledHot = byServerUtils()
        ServerHotLoadSupport(disabledHot).onDisable()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.removeModule(it) }
        commandManager.shutdown()

        for (player in server.allPlayers) {
            VelocityUserManager.getCache(player.uniqueId)?.let {
                // We don't update user there, backend server will do it
                VelocityUserManager.unload(it)
            }
        }
        UpdateCheckerMan.shutdown()
        StorageManager.shutdown()
        server.eventManager.unregisterListeners(container)
        try {
            @OptIn(DelicateCoroutinesApi::class) // Just release the resources
            Dispatchers.shutdown()
        } catch (t: Throwable) {
            err("An exception occurred while shutting down coroutine: $t")
        }
    }

    @Subscribe(order = PostOrder.LAST)
    fun onLogin(event: PostLoginEvent) {
        if (event.player.isActive) // Player may be kicked
            UpdateCheckerMan.onJoin(VelocityUserManager[event.player])
    }

    @Subscribe(order = PostOrder.LAST)
    fun onQuit(event: DisconnectEvent) {
        VelocityUserManager.getCache(event.player.uniqueId)?.let {
            VelocityUserManager.unload(it)
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    fun onReceiveConfiguration(event: PlayerSettingsChangedEvent) {
        event.player.user.onSettingsReceived()
    }

    @Subscribe
    fun blockPlayerFakeEsuPluginMessage(e: PluginMessageEvent) {
        if (e.source !is Player) return
        when (val identifier = e.identifier) {
            is MinecraftChannelIdentifier -> {
                if (identifier.id == "esu") e.result = PluginMessageEvent.ForwardResult.handled()
            }
            is LegacyChannelIdentifier -> {
                if (identifier.name == "esu") e.result = PluginMessageEvent.ForwardResult.handled()
            }
            else -> throw IllegalStateException("Unsupported plugin identifier type: $identifier")
        }
    }

    private fun byServerUtils(): Boolean {
        var found = false
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            val name = it.declaringClass.canonicalName
            if (found || (name != null && name.contains("serverutils"))) {
                found = true
            }
        }
        return found
    }

    internal class ServerHotLoadSupport(isHot: Boolean) : HotLoadSupport(isHot)

}