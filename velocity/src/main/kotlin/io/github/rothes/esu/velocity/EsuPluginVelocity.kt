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
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.colorscheme.ColorSchemes
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.command.parser.ModuleParser
import io.github.rothes.esu.core.config.EsuConfig
import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.storage.StorageManager
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.core.util.artifact.AetherLoader
import io.github.rothes.esu.core.util.artifact.MavenResolver
import io.github.rothes.esu.core.util.artifact.relocator.CachedRelocator
import io.github.rothes.esu.core.util.artifact.relocator.PackageRelocator
import io.github.rothes.esu.velocity.command.parser.UserParser
import io.github.rothes.esu.velocity.config.VelocityEsuLocale
import io.github.rothes.esu.velocity.module.AutoReloadExtensionPluginsModule
import io.github.rothes.esu.velocity.module.NetworkThrottleModule
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

class EsuPluginVelocity(
    val bootstrap: EsuBootstrap,
): EsuCore {

    override var dependenciesResolved: Boolean = false
        private set
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

        AetherLoader
        MavenResolver.loadDependencies(
            listOf(
                "org.jetbrains.exposed:exposed-core:${BuildConfig.EXPOSED_VERSION}",
                "org.jetbrains.exposed:exposed-jdbc:${BuildConfig.EXPOSED_VERSION}",
                "org.jetbrains.exposed:exposed-kotlin-datetime:${BuildConfig.EXPOSED_VERSION}",
                "org.jetbrains.exposed:exposed-json:${BuildConfig.EXPOSED_VERSION}",

                "com.zaxxer:HikariCP:6.3.0",
                "org.incendo:cloud-core:2.0.0",
                "org.incendo:cloud-annotations:2.0.0",
                "org.incendo:cloud-kotlin-coroutines-annotations:2.0.0",

                "org.incendo:cloud-velocity:2.0.0-beta.10",

                "com.h2database:h2:2.3.232",
                "com.mysql:mysql-connector-j:8.4.0",
                "org.mariadb.jdbc:mariadb-java-client:3.5.3",

                "org.ow2.asm:asm-commons:9.8",
            )
        )
        val relocator = PackageRelocator("net/kyori/" to "io/github/rothes/esu/lib/net/kyori/")
        MavenResolver.loadDependencies(
            listOf(
                "net.kyori:adventure-api:${BuildConfig.ADVENTURE_VERSION}",
                "net.kyori:adventure-text-minimessage:${BuildConfig.ADVENTURE_VERSION}",
                "net.kyori:adventure-text-serializer-ansi:${BuildConfig.ADVENTURE_VERSION}",
                "net.kyori:adventure-text-serializer-gson:${BuildConfig.ADVENTURE_VERSION}",
                "net.kyori:adventure-text-serializer-legacy:${BuildConfig.ADVENTURE_VERSION}",
                "net.kyori:adventure-text-serializer-plain:${BuildConfig.ADVENTURE_VERSION}",
            )
        ) { file, artifact ->
            if (artifact.groupId == "net.kyori")
                CachedRelocator.relocate(relocator, file)
            else
                file
        }
        dependenciesResolved = true
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
        EsuConfig           // Load global config, in case of. MavenResolver should init it tho.
        VelocityEsuLocale   // Load global locale
        StorageManager      // Load database
        ColorSchemes        // Load color schemes
        UpdateCheckerMan    // Init update checker

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

        bootstrap.metricsFactory.make(bootstrap, 24826) // bStats

        initialized = true
        enabled = true
    }

    @Subscribe
    fun onDisable(e: ProxyShutdownEvent) {
        enabled = false
        disabledHot = byServerUtils()
        ModuleManager.registeredModules().filter { it.enabled }.reversed().forEach { ModuleManager.disableModule(it) }

        for (player in server.allPlayers) {
            VelocityUserManager.getCache(player.uniqueId)?.let {
                // We don't update user there, backend server will do it
                VelocityUserManager.unload(it)
            }
        }
        UpdateCheckerMan.shutdown()
        StorageManager.shutdown()
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