package io.github.rothes.esu.velocity

import cc.carm.lib.easysql.hikari.HikariDataSource
import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PreLoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.velocity.command.parser.UserParser
import io.github.rothes.esu.velocity.config.VelocityEsuLocale
import io.github.rothes.esu.velocity.module.UserNameVerifyModule
import io.github.rothes.esu.velocity.user.ConsoleUser
import io.github.rothes.esu.velocity.user.VelocityUser
import io.github.rothes.esu.velocity.user.VelocityUserManager
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import org.incendo.cloud.velocity.VelocityCommandManager
import org.slf4j.Logger
import java.nio.file.Path

private const val PLUGIN_ID = "esu"

@Plugin(
    id = PLUGIN_ID,
    name = "ESU",
    version = BuildConfig.VERSION_NAME,
    authors = ["Rothes"],
    url = "https://github.com/Rothes/ESU",
)
class EsuPluginVelocity @Inject constructor(
    val server: ProxyServer, val logger: Logger, @DataDirectory private val dataDirectory: Path
): EsuCore {

    override var initialized: Boolean = false
        private set

    private val container: PluginContainer by lazy {
        server.pluginManager.getPlugin(PLUGIN_ID).get()
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


    @Subscribe
    fun onProxyInitialization(e: ProxyInitializeEvent) {
        EsuCore.instance = this
        EsuConfig // Load global config
        VelocityEsuLocale // Load global locale
        StorageManager // Load database
        ColorSchemes // Load color schemes

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
                        ModuleManager.registeredModules().forEach { module -> module.reloadConfig() }
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

        initialized = true
    }

    @Subscribe
    fun onDisable(e: ProxyShutdownEvent) {
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.disableModule(it) }
        (StorageManager.sqlManager.dataSource as HikariDataSource).close()
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
    fun onQuit(event: DisconnectEvent) {
        VelocityUserManager.getCache(event.player.uniqueId)?.let {
            VelocityUserManager.unload(it)
        }
    }

    override fun info(message: String) {
        logger.info(message)
    }

    override fun warn(message: String) {
        logger.warn(message)
    }

    override fun err(message: String) {
        logger.error(message)
    }

    override fun err(message: String, throwable: Throwable?) {
        logger.error(message, throwable)
    }

    override fun baseConfigPath(): Path {
        return dataDirectory
    }

}