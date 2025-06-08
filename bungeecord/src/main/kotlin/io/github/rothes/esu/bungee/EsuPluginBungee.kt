package io.github.rothes.esu.bungee

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.bungee.command.parser.UserParser
import io.github.rothes.esu.bungee.config.BungeeEsuLocale
import io.github.rothes.esu.bungee.user.BungeeUser
import io.github.rothes.esu.bungee.user.BungeeUserManager
import io.github.rothes.esu.bungee.user.ConsoleUser
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.api.plugin.Plugin
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import java.nio.file.Path
import java.util.logging.Level

class EsuPluginBungee: Plugin(), EsuCore {

    override var initialized: Boolean = false
        private set
    var enabled: Boolean = false
        private set
    var adventure: BungeeAudiences? = null
        private set

    var enabledHot: Boolean by InitOnce()
    var disabledHot: Boolean by InitOnce()

    override val commandManager: BungeeCommandManager<BungeeUser> by lazy {
        BungeeCommandManager(this, ExecutionCoordinator.asyncCoordinator(), SenderMapper.create({
            when (it) {
                ProxyServer.getInstance().console -> ConsoleUser
                is ProxiedPlayer                  -> it.user
                else                              -> throw IllegalArgumentException("Unsupported user type: ${it.javaClass.name}")
            }
        }, { it.commandSender })).apply {
            settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)
            captionRegistry().registerProvider { caption, recipient ->
                recipient.localedOrNull(BungeeEsuLocale.get()) {
                    commandCaptions[caption]
                }
            }
            parserRegistry().registerParser(UserParser.parser())
            parserRegistry().registerNamedParser("greedyString", StringParser.greedyStringParser())
            EsuExceptionHandlers(exceptionController()).register()
        }
    }

    override fun onEnable() {
        EsuCore.instance = this
        adventure = BungeeAudiences.create(this)
        enabledHot = byServerUtils()
        EsuConfig // Load global config
        BungeeEsuLocale // Load global locale
        StorageManager // Load database
        ColorSchemes // Load color schemes

        // Register commands
        with(commandManager) {
            val esu = commandBuilder("besu", Description.of("ESU Bungeecord commands")).permission("besu.command.admin")
            command(
                esu.literal("reload")
                    .handler { context ->
                        EsuConfig.reloadConfig()
                        BungeeEsuLocale.reloadConfig()
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

        ProxyServer.getInstance().players.forEach { it.user }
        ProxyServer.getInstance().pluginManager.registerListener(plugin, object : Listener {

            @EventHandler(priority = EventPriority.HIGHEST)
            fun onLogin(event: PostLoginEvent) {
                BungeeUserManager[event.player]
            }
            @EventHandler(priority = EventPriority.HIGHEST)
            fun onQuit(event: PlayerDisconnectEvent) {
                BungeeUserManager.getCache(event.player.uniqueId)?.let {
                    StorageManager.updateUserAsync(it)
                    BungeeUserManager.unload(it)
                }
            }
        })

        initialized = true
        enabled = true
    }

    override fun onDisable() {
        enabled = false
        disabledHot = byServerUtils()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.disableModule(it) }

        for (player in ProxyServer.getInstance().players) {
            BungeeUserManager.getCache(player.uniqueId)?.let {
                // We don't update user there, backend server will do it
                BungeeUserManager.unload(it)
            }
        }
        StorageManager.shutdown()
        adventure?.close()?.also { adventure = null }
    }

    override fun info(message: String) {
        logger.log(Level.INFO, message)
    }

    override fun warn(message: String) {
        logger.log(Level.WARNING, message)
    }

    override fun err(message: String) {
        logger.log(Level.SEVERE, message)
    }

    override fun err(message: String, throwable: Throwable?) {
        logger.log(Level.SEVERE, message, throwable)
    }

    override fun baseConfigPath(): Path {
        return dataFolder.toPath()
    }

    private fun byServerUtils(): Boolean {
        var found = false
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            if (found || it.declaringClass.canonicalName.contains("serverutils")) {
                found = true
            }
        }
        return found
    }

}