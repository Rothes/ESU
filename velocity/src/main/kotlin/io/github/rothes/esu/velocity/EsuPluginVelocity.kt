package io.github.rothes.esu.velocity

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.proxy.connection.client.ConnectedPlayer
import io.github.rothes.esu.common.HotLoadSupport
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.core.util.ReflectionUtils.accessibleGetT
import io.github.rothes.esu.lib.packetevents.PacketEvents
import io.github.rothes.esu.lib.packetevents.injector.ServerConnectionInitializer
import io.github.rothes.esu.lib.packetevents.protocol.ConnectionState
import io.github.rothes.esu.lib.packetevents.velocity.factory.VelocityPacketEventsBuilder
import io.github.rothes.esu.velocity.command.parser.UserParser
import io.github.rothes.esu.velocity.config.VelocityEsuLocale
import io.github.rothes.esu.velocity.module.AutoReloadExtensionPluginsModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.UserNameVerifyModule
import io.github.rothes.esu.velocity.module.networkthrottle.channel.Injector
import io.github.rothes.esu.velocity.user.ConsoleUser
import io.github.rothes.esu.velocity.user.VelocityUser
import io.github.rothes.esu.velocity.user.VelocityUserManager
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import org.incendo.cloud.velocity.VelocityCommandManager
import org.slf4j.Logger
import java.nio.file.Path

class EsuPluginVelocity(
    val bootstrap: EsuBootstrapVelocity,
): EsuCore {

    override var initialized: Boolean = false
        private set
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

    override val commandManager: VelocityCommandManager<VelocityUser> by lazy {
        VelocityCommandManager(container, server, ExecutionCoordinator.asyncCoordinator(), SenderMapper.create({
            when (it) {
                is ConsoleCommandSource -> ConsoleUser
                is Player               -> it.user
                else                    -> throw IllegalArgumentException("Unsupported user type: ${it.javaClass.name}")
            }
        }, { it.commandSender })).apply {
            settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)
            captionRegistry().registerProvider { caption, recipient ->
                recipient.localedOrNull(VelocityEsuLocale.get()) {
                    commandCaptions[caption]
                }
            }
            parserRegistry().registerParser(UserParser.parser())
            parserRegistry().registerNamedParser("greedyString", StringParser.greedyStringParser())
            EsuExceptionHandlers(exceptionController()).register()
        }
    }

    fun onProxyInitialization() {
        EsuConfig           // Load global config
        VelocityEsuLocale   // Load global locale
        StorageManager      // Load database
        ColorSchemes        // Load color schemes
        UpdateCheckerMan    // Init update checker

        PacketEvents.setAPI(VelocityPacketEventsBuilder.build(server, container, logger, dataDirectory))
        val hotLoadSupport = HotLoadSupport(enabledHot)
        PacketEvents.getAPI().load()
        hotLoadSupport.onEnable()
        for (player in server.allPlayers) {
            val channel = PacketEvents.getAPI().playerManager.getChannel(player) as Channel
            ServerConnectionInitializer.initChannel(channel, ConnectionState.HANDSHAKING)
            hotLoadSupport.loadPEUser(player, player.uniqueId, player.username)
        }
        PacketEvents.getAPI().init()

        ModuleManager.addModule(AutoReloadExtensionPluginsModule)
        ModuleManager.addModule(NetworkThrottleModule)
        ModuleManager.addModule(UserNameVerifyModule)

        // Register commands
        with(commandManager) {
            val esu = commandBuilder("vesu", Description.of("ESU Velocity commands")).permission("vesu.command.admin")
            command(
                esu.literal("reload")
                    .handler { context ->
                        EsuConfig.reloadConfig()
                        VelocityEsuLocale.reloadConfig()
                        ColorSchemes.reload()
                        UpdateCheckerMan.reload()
                        ModuleManager.reloadModules()
                        context.sender().message("§aReloaded global & module configs.")
                    }
            )
            val moduleCmd = esu.literal("module")
            command(
                moduleCmd.literal("enable")
                    .required("module", ModuleParser.parser(), ModuleParser())
                    .handler { context ->
                        val module = context.get<Module<*, *>>("module")
                        if (module.enabled) {
                            context.sender().message("§2Module ${module.name} is already enabled.")
                        } else if (ModuleManager.enableModule(module)) {
                            context.sender().message("§aModule ${module.name} is enabled.")
                        } else {
                            context.sender().message("§cFailed to enable module ${module.name}.")
                        }
                    }
            )
            command(
                moduleCmd.literal("disable")
                    .required("module", ModuleParser.parser(), ModuleParser())
                    .handler { context ->
                        val module = context.get<Module<*, *>>("module")
                        if (!module.enabled) {
                            context.sender().message("§4Module ${module.name} is already disabled.")
                        } else if (ModuleManager.disableModule(module)) {
                            context.sender().message("§cModule ${module.name} is disabled.")
                        } else {
                            context.sender().message("§cFailed to disable module ${module.name}.")
                        }
                    }
            )
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
        HotLoadSupport(disabledHot).onDisable()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.disableModule(it) }

        for (player in server.allPlayers) {
            VelocityUserManager.getCache(player.uniqueId)?.let {
                // We don't update user there, backend server will do it
                VelocityUserManager.unload(it)
            }
        }
        UpdateCheckerMan.shutdown()
        StorageManager.shutdown()
        shutdownPacketEvents()
        server.eventManager.unregisterListeners(container)
    }

    @Subscribe(order = PostOrder.LAST)
    fun onLogin(event: PreLoginEvent) {
        event.uniqueId?.let { VelocityUserManager[it] }
    }

    @Subscribe(order = PostOrder.LAST)
    fun onLogin(event: LoginEvent) {
        VelocityUserManager[event.player]
    }

    @Subscribe(order = PostOrder.LAST)
    fun onLogin(event: PostLoginEvent) {
        UpdateCheckerMan.onJoin(VelocityUserManager[event.player])
    }

    @Subscribe(order = PostOrder.LAST)
    fun onQuit(event: DisconnectEvent) {
        VelocityUserManager.getCache(event.player.uniqueId)?.let {
            VelocityUserManager.unload(it)
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

    private fun shutdownPacketEvents() {
        PacketEvents.getAPI().terminate()
        for (player in plugin.server.allPlayers) {
            try {
                val channel = (player as ConnectedPlayer).connection.channel
                channel.pipeline().remove(PacketEvents.ENCODER_NAME)
                channel.pipeline().remove(PacketEvents.DECODER_NAME)
            } catch (e: Exception) {
                err("Failed to uninject packetevents for player ${player.username}", e)
            }
        }
        try {
            val initializer = Injector.connectionManager.serverChannelInitializer.get()
            if (initializer is io.github.rothes.esu.lib.packetevents.injector.VelocityChannelInitializer) {
                Injector.connectionManager.serverChannelInitializer.set(
                    initializer::class.java.declaredFields
                        .first { it.type == ChannelInitializer::class.java }
                        .accessibleGetT(initializer)
                )
            }
        } catch (e: Exception) {
            err("Failed to uninject packetevents VelocityChannelInitializer", e)
        }
    }

}