package io.github.rothes.esu.velocity

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import io.github.rothes.esu.common.HotLoadSupport
import io.github.rothes.esu.common.module.AutoBroadcastModule
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.velocity.command.parser.UserParser
import io.github.rothes.esu.velocity.config.VelocityEsuLang
import io.github.rothes.esu.velocity.module.AutoReloadExtensionPluginsModule
import io.github.rothes.esu.velocity.module.AutoRestartModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
import io.github.rothes.esu.velocity.module.UserNameVerifyModule
import io.github.rothes.esu.velocity.user.ConsoleUser
import io.github.rothes.esu.velocity.user.VelocityUserManager
import kotlinx.coroutines.Dispatchers
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

    override val commandManager: VelocityCommandManager<User> by lazy {
        VelocityCommandManager<User>(container, server, ExecutionCoordinator.asyncCoordinator(), SenderMapper.create({
            when (it) {
                is ConsoleCommandSource -> ConsoleUser
                is Player               -> it.user
                else                    -> throw IllegalArgumentException("Unsupported user type: ${it.javaClass.name}")
            }
        }, { it.commandSender as CommandSource })).apply {
            settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)
            captionRegistry().registerProvider { caption, recipient ->
                recipient.localedOrNull(VelocityEsuLang.get()) {
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
        ModuleManager.addModule(UserNameVerifyModule)
        ModuleManager.addModule(AutoReloadExtensionPluginsModule)

        // Register commands
        with(commandManager) {
            val esu = commandBuilder("vesu", Description.of("ESU Velocity commands")).permission("vesu.command.admin")
            command(
                esu.literal("reload")
                    .handler { context ->
                        EsuConfig.reloadConfig()
                        VelocityEsuLang.reloadConfig()
                        ColorSchemes.reload()
                        UpdateCheckerMan.reload()
                        ModuleManager.reloadModules()
                        context.sender().message("§aReloaded global & module configs.")
                    }
            )
            val moduleCmd = esu.literal("module")
            command(
                moduleCmd.literal("forceEnable")
                    .required("module", ModuleParser.parser(), ModuleParser())
                    .handler { context ->
                        val module = context.get<Module<*, *>>("module")
                        if (module.enabled) {
                            context.sender().message("§2Module ${module.name} is already enabled.")
                        } else if (ModuleManager.forceEnableModule(module)) {
                            context.sender().message("§aModule ${module.name} is enabled.")
                        } else {
                            context.sender().message("§cFailed to enable module ${module.name}.")
                        }
                    }
            )
            command(
                moduleCmd.literal("forceDisable")
                    .required("module", ModuleParser.parser(), ModuleParser())
                    .handler { context ->
                        val module = context.get<Module<*, *>>("module")
                        if (!module.enabled) {
                            context.sender().message("§4Module ${module.name} is already disabled.")
                        } else if (ModuleManager.forceDisableModule(module)) {
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
        ServerHotLoadSupport(disabledHot).onDisable()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.removeModule(it) }

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
            Dispatchers.shutdown()
        } catch (t: Throwable) {
            err("An exception occurred while shutting down coroutine: $t")
        }
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

    internal class ServerHotLoadSupport(isHot: Boolean) : HotLoadSupport(isHot)

}