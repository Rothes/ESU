package io.github.rothes.esu.bukkit

import cc.carm.lib.easysql.hikari.HikariDataSource
import io.github.rothes.esu.bukkit.command.parser.UserParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.bukkit.config.BukkitEsuLocale
import io.github.rothes.esu.bukkit.module.AntiCommandSpamModule
import io.github.rothes.esu.bukkit.module.AutoReloadExtensionPluginsModule
import io.github.rothes.esu.bukkit.module.AutoRestartModule
import io.github.rothes.esu.bukkit.module.BetterEventMessagesModule
import io.github.rothes.esu.bukkit.module.BlockedCommandsModule
import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule
import io.github.rothes.esu.bukkit.module.EsuChatModule
import io.github.rothes.esu.bukkit.module.ItemEditModule
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule
import io.github.rothes.esu.bukkit.module.NewbieProtectModule
import io.github.rothes.esu.bukkit.module.SpoofServerSettingsModule
import io.github.rothes.esu.bukkit.module.UtilCommandsModule
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.GenericUser
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.InitOnce
import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bukkit.BukkitCommandManager
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import java.nio.file.Path
import java.util.logging.Level

class EsuPluginBukkit: JavaPlugin(), EsuCore {

    override var initialized: Boolean = false
        private set

    var enabledHot: Boolean by InitOnce()
    var disabledHot: Boolean by InitOnce()

    override val commandManager: BukkitCommandManager<BukkitUser> by lazy {
        LegacyPaperCommandManager(this, ExecutionCoordinator.asyncCoordinator(), SenderMapper.create({
            when (it) {
                is ConsoleCommandSender -> ConsoleUser
                is Player               -> it.user
                else                    -> GenericUser(it)
            }
        }, { it.commandSender })).apply {
            settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)
            captionRegistry().registerProvider { caption, recipient ->
                recipient.localedOrNull(BukkitEsuLocale.get()) {
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
        enabledHot = byPluginMan()
        EsuConfig // Load global config
        BukkitEsuLocale // Load global locale
        StorageManager // Load database
        ColorSchemes // Load color schemes

        ModuleManager.addModule(AntiCommandSpamModule)
        ModuleManager.addModule(AutoReloadExtensionPluginsModule)
        ModuleManager.addModule(AutoRestartModule)
        ModuleManager.addModule(BetterEventMessagesModule)
        ModuleManager.addModule(BlockedCommandsModule)
        ModuleManager.addModule(ChatAntiSpamModule)
        ModuleManager.addModule(EsuChatModule)
        ModuleManager.addModule(ItemEditModule)
        ModuleManager.addModule(NetworkThrottleModule)
        ModuleManager.addModule(NewbieProtectModule)
        ModuleManager.addModule(SpoofServerSettingsModule)
        ModuleManager.addModule(UtilCommandsModule)

        // Register commands
        with(commandManager) {
            val esu = commandBuilder("esu", Description.of("ESU commands")).permission("esu.command.admin")
            command(
                esu.literal("reload")
                    .handler { context ->
                        EsuConfig.reloadConfig()
                        BukkitEsuLocale.reloadConfig()
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

        Bukkit.getOnlinePlayers().forEach { it.user }
        Bukkit.getPluginManager().registerEvents(object : Listener {

            @EventHandler(priority = EventPriority.MONITOR)
            fun onLogin(event: AsyncPlayerPreLoginEvent) {
                if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED)
                    BukkitUserManager[event.uniqueId]
                else
                    BukkitUserManager.unload(event.uniqueId)
            }

            @EventHandler(priority = EventPriority.MONITOR)
            fun onQuit(event: PlayerQuitEvent) {
                BukkitUserManager.getCache(event.player.uniqueId)?.let {
                    StorageManager.updateUserNameAsync(it)
                    BukkitUserManager.unload(it)
                }
            }
        }, this)

        Scheduler.global {
            initialized = true
        }
    }

    override fun onDisable() {
        disabledHot = byPluginMan()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.disableModule(it) }
        (StorageManager.sqlManager.dataSource as HikariDataSource).close()
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

    override fun err(message: String, throwable: Throwable) {
        logger.log(Level.SEVERE, message, throwable)
    }

    override fun baseConfigPath(): Path {
        return dataFolder.toPath()
    }

    private fun byPluginMan(): Boolean {
        var found = false
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            if (found || it.declaringClass.canonicalName.contains("plugman")) {
                found = true
            }
        }
        return found
    }

}