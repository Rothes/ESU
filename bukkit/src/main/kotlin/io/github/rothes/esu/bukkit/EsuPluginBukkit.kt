package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.command.parser.UserParser
import io.github.rothes.esu.bukkit.command.parser.location.ChunkLocationParser
import io.github.rothes.esu.bukkit.config.BukkitEsuLang
import io.github.rothes.esu.bukkit.event.UserLoginEvent
import io.github.rothes.esu.bukkit.event.internal.InternalListeners
import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import io.github.rothes.esu.bukkit.module.*
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.GenericUser
import io.github.rothes.esu.bukkit.util.BukkitDataSerializer
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter.Companion.topInv
import io.github.rothes.esu.bukkit.util.version.remapper.JarRemapper
import io.github.rothes.esu.common.HotLoadSupport
import io.github.rothes.esu.common.util.extension.shutdown
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
import io.github.rothes.esu.core.util.extension.ClassExt.jarFile
import io.github.rothes.esu.lib.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bukkit.BukkitCommandManager
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting

class EsuPluginBukkit(
    val bootstrap: EsuBootstrapBukkit
): EsuCore {

    override var initialized: Boolean = false
        private set

    override val basePermissionNode: String = "esu"

    var enabledHot: Boolean by InitOnce()
    var disabledHot: Boolean by InitOnce()

    init {
        EsuCore.instance = this
        BukkitDataSerializer // Register bukkit serializers

        loadVersions()
        Class.forName("io.github.rothes.esu.bukkit.AnsiFlattener")
        enabledHot = byPlugMan()
    }

    private fun loadVersions() {
        val tempFolder = bootstrap.dataFolder.resolve(".cache/minecraft_versions")
        tempFolder.deleteRecursively()
        tempFolder.mkdirs()

        javaClass.jarFile.use { jarFile ->
            val entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val fullName = entry.name
                if (fullName.startsWith("esu_minecraft_versions/") && fullName != "esu_minecraft_versions/") {
                    val name = fullName.substringAfterLast("/")
                    val file = tempFolder.resolve(name)
                    file.createNewFile()

                    jarFile.getInputStream(entry).use { stream ->
                        file.outputStream().use { output ->
                            stream.copyTo(output)
                        }
                    }

                    val toLoad = if (!ServerCompatibility.isMojmap && ServerCompatibility.hasMojmap) JarRemapper.reobf(file) else file
                    Versioned.loadVersion(toLoad)
                }
            }
        }
    }

    override val commandManager: BukkitCommandManager<User> by lazy {
        LegacyPaperCommandManager<User>(bootstrap, ExecutionCoordinator.asyncCoordinator(), SenderMapper.create({
            when (it) {
                is ConsoleCommandSender -> ConsoleUser
                is Player               -> it.user
                else                    -> GenericUser(it)
            }
        }, { it.commandSender as CommandSender })).apply {
            settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)
            captionRegistry().registerProvider { caption, recipient ->
                recipient.localedOrNull(BukkitEsuLang.get()) {
                    commandCaptions[caption]
                }
            }
            parserRegistry().registerParser(ChunkLocationParser.parser())
            parserRegistry().registerParser(UserParser.parser())
            parserRegistry().registerNamedParser("greedyString", StringParser.greedyStringParser())
            EsuExceptionHandlers(exceptionController()).register()
        }
    }

    fun onLoad() {
    }

    fun onEnable() {
        adventure           // Init adventure
        EsuConfig           // Load global config
        BukkitEsuLang     // Load global locale
        StorageManager      // Load database
        ColorSchemes        // Load color schemes
        UpdateCheckerMan    // Init update checker

        ServerHotLoadSupport(enabledHot).onEnable()

        ModuleManager.addModule(AutoReloadExtensionPluginsModule)
        ModuleManager.addModule(AutoRestartModule)
        ModuleManager.addModule(BetterEventMessagesModule)
        ModuleManager.addModule(BlockedCommandsModule)
        ModuleManager.addModule(ChatAntiSpamModule)
        ModuleManager.addModule(CommandAntiSpamModule)
        ModuleManager.addModule(EsuChatModule)
        ModuleManager.addModule(ExploitFixesModule)
        ModuleManager.addModule(ItemEditModule)
        ModuleManager.addModule(NetworkThrottleModule)
        ModuleManager.addModule(SocialFilterModule)
        ModuleManager.addModule(SpawnProtectModule)
        ModuleManager.addModule(NewsModule)
        ModuleManager.addModule(OptimizationsModule)
        ModuleManager.addModule(SpoofServerSettingsModule)
        ModuleManager.addModule(UtilCommandsModule)

        // Register commands
        with(commandManager) {
            val esu = commandBuilder("esu", Description.of("ESU commands")).permission("esu.command.admin")
            command(
                esu.literal("reload")
                    .handler { context ->
                        EsuConfig.reloadConfig()
                        BukkitEsuLang.reloadConfig()
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

        Bukkit.getOnlinePlayers().forEach { it.user }
        InternalListeners // Init
        UserLoginEvent // Init

        Metrics(bootstrap, 24645) // bStats
        Scheduler.global {
            initialized = true
        }
    }

    fun onDisable() {
        disabledHot = byPlugMan()
        ServerHotLoadSupport(disabledHot).onDisable()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.removeModule(it) }
        commandManager.shutdown()

        for (player in Bukkit.getOnlinePlayers()) {
            try {
                val inventoryHolder = player.openInventory.topInv.holder
                if (inventoryHolder is EsuInvHolder<*>) {
                    inventoryHolder.close()
                }
            } catch (_: IllegalStateException) {
                // Cannot read world asynchronously on Folia, when player is opening a world inv
            }

            BukkitUserManager.getCache(player.uniqueId)?.let { user ->
                try {
                    StorageManager.updateUserNow(user)
                } catch (t: Throwable) {
                    err("Failed to update user ${user.nameUnsafe}(${user.uuid}) on disable", t)
                }
                BukkitUserManager.unload(user)
            }
        }
        UpdateCheckerMan.shutdown()
        StorageManager.shutdown()
        adventure.close()
    }

    private fun byPlugMan(): Boolean {
        var found = false
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).forEach {
            val name = it.declaringClass.canonicalName
            if (found || (name != null && name.contains("plugman"))) {
                found = true
            }
        }
        return found
    }

    // JavaPlugin methods
    val isEnabled
        get() = bootstrap.isEnabled
    @Suppress("DEPRECATION")
    val description
        get() = bootstrap.description

    internal class ServerHotLoadSupport(isHot: Boolean) : HotLoadSupport(isHot)

}