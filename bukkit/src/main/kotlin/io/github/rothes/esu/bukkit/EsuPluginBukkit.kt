package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.command.parser.UserParser
import io.github.rothes.esu.bukkit.config.BukkitEsuLocale
import io.github.rothes.esu.bukkit.event.UserLoginEvent
import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import io.github.rothes.esu.bukkit.module.*
import io.github.rothes.esu.bukkit.user.BukkitUser
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.user.GenericUser
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.artifact.MavenResolver
import io.github.rothes.esu.bukkit.util.artifact.injector.UnsafeURLInjector
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.InventoryAdapter.Companion.topInv
import io.github.rothes.esu.bukkit.util.version.remapper.FileHashes.Companion.sha1
import io.github.rothes.esu.bukkit.util.version.remapper.JarRemapper
import io.github.rothes.esu.bukkit.util.version.remapper.MappingsLoader
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.InitOnce
import net.jpountz.lz4.LZ4Factory
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bukkit.BukkitCommandManager
import org.incendo.cloud.description.Description
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.logging.Level

class EsuPluginBukkit: JavaPlugin(), EsuCore {

    override var initialized: Boolean = false
        private set

    var enabledHot: Boolean by InitOnce()
    var disabledHot: Boolean by InitOnce()

    init {
        EsuCore.instance = this
        if (!ServerCompatibility.mojmap) {
            info("You are not running a Mojmap server, loading necessary libraries...")
            loadAether()
            MavenResolver.loadDependencies(
                listOf(
                    "net.neoforged:AutoRenamingTool:2.0.13",
                )
            )
            MappingsLoader
        }
        if (!ServerCompatibility.paper) {
            info("You are not running a Paper server, loading necessary libraries...")
            MavenResolver.loadDependencies(
                listOf(
                    "net.kyori:adventure-platform-bukkit:4.4.1",
                    "net.kyori:adventure-api:4.24.0",
                    "net.kyori:adventure-text-minimessage:4.24.0",
                    "net.kyori:adventure-text-serializer-ansi:4.24.0",
                    "net.kyori:adventure-text-serializer-gson:4.24.0",
                    "net.kyori:adventure-text-serializer-legacy:4.24.0",
                    "net.kyori:adventure-text-serializer-plain:4.24.0",
                )
            )
        }
        info("Checking missing libraries...")
        MavenResolver.testDependency("org.lz4:lz4-java:1.8.0") {
            LZ4Factory.fastestInstance()
        }

        loadVersions()
        enabledHot = byPluginMan()
    }

    private fun loadVersions() {
        val tempFolder = dataFolder.resolve(".cache/minecraft_versions")
        tempFolder.deleteRecursively()
        tempFolder.mkdirs()

        val jarFile = JarFile(javaClass.protectionDomain.codeSource.location.path)
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val fullName = entry.name
            if (fullName.startsWith("esu_minecraft_versions/") && fullName != "esu_minecraft_versions/") {
                val url = classLoader.getResource(fullName)!!

                val name = fullName.substringAfterLast("/")
                val file = tempFolder.resolve(name)
                file.createNewFile()
                url.openStream().use { stream ->
                    file.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                }

                val toLoad = if (!ServerCompatibility.mojmap) JarRemapper.reobf(file) else file
                Versioned.loadVersion(toLoad)
            }
        }
    }

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
        checkSpigotSupport()
        EsuConfig           // Load global config
        BukkitEsuLocale     // Load global locale
        StorageManager      // Load database
        ColorSchemes        // Load color schemes
        UpdateCheckerMan    // Init update checker

        ModuleManager.addModule(AntiCommandSpamModule)
        ModuleManager.addModule(AutoReloadExtensionPluginsModule)
        ModuleManager.addModule(AutoRestartModule)
        ModuleManager.addModule(BetterEventMessagesModule)
        ModuleManager.addModule(BlockedCommandsModule)
        ModuleManager.addModule(ChatAntiSpamModule)
        ModuleManager.addModule(EsuChatModule)
        ModuleManager.addModule(ExploitFixModule)
        ModuleManager.addModule(ItemEditModule)
        ModuleManager.addModule(NetworkThrottleModule)
        ModuleManager.addModule(NewbieProtectModule)
        ModuleManager.addModule(NewsModule)
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
                        UpdateCheckerMan.reload()
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
            @EventHandler(priority = EventPriority.LOWEST)
            fun onLogin(event: PlayerJoinEvent) {
                val user = BukkitUserManager[event.player]
                UpdateCheckerMan.onJoin(user)
            }
            @EventHandler(priority = EventPriority.MONITOR)
            fun onQuit(event: PlayerQuitEvent) {
                BukkitUserManager.getCache(event.player.uniqueId)?.let {
                    StorageManager.updateUserAsync(it)
                    BukkitUserManager.unload(it)
                }
            }

            @EventHandler(priority = EventPriority.LOWEST)
            fun onClick(e: InventoryClickEvent) {
                val holder = e.inventory.holder
                if (holder is EsuInvHolder<*>) {
                    holder.handleClick(e)
                }
            }
            @EventHandler(priority = EventPriority.LOWEST)
            fun onClick(e: InventoryDragEvent) {
                val holder = e.inventory.holder
                if (holder is EsuInvHolder<*>) {
                    holder.handleDrag(e)
                }
            }
        }, this)
        UserLoginEvent // Init

        Metrics(this, 24645) // bStats
        Scheduler.global {
            initialized = true
        }
    }

    override fun onDisable() {
        disabledHot = byPluginMan()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.disableModule(it) }

        for (player in Bukkit.getOnlinePlayers()) {
            try {
                val inventoryHolder = player.openInventory.topInv.holder
                if (inventoryHolder is EsuInvHolder<*>) {
                    inventoryHolder.close()
                }
            } catch (_: IllegalStateException) {
                // Cannot read world asynchronously on Folia, when player is opening a world inv
            }

            BukkitUserManager.getCache(player.uniqueId)?.let {
                StorageManager.updateUserNow(it)
                BukkitUserManager.unload(it)
            }
        }
        UpdateCheckerMan.shutdown()
        StorageManager.shutdown()
        disableSpigotSupport()
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

    private fun checkSpigotSupport() {
        if (ServerCompatibility.paper)
            return
        ServerCompatibility.CB
    }

    private fun disableSpigotSupport() {
        if (ServerCompatibility.paper)
            return
        ServerCompatibility.CB.adventure.close()
    }

    private fun loadAether() {
        try {
            Class.forName("org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory")
        } catch (_: ClassNotFoundException) {
            // Spigot 1.16.5 and older
            val resolve = plugin.dataFolder.resolve(".cache/aether-library.jar")
            if (!resolve.exists() || resolve.sha1 != "f2bbafed1dd38ffdbaac1daf17ca706efbec74ef") {
                fun downloadAetherLib(domain: String) {
                    val url = URI.create("https://$domain/Rothes/ESU/blob/raw/aether-library.jar").toURL()
                    info("Downloading $url to $resolve")
                    resolve.createNewFile()
                    url.openStream().use { stream ->
                        resolve.outputStream().use { outputStream ->
                            stream.copyTo(outputStream)
                        }
                    }
                }
                try {
                    downloadAetherLib("github.com")
                } catch (_: IOException) {
                    info("Connection error, fallback to another link")
                    downloadAetherLib("ghfast.top/https://github.com")
                }
            }
            UnsafeURLInjector.addURL(resolve.toURI().toURL())
        }
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