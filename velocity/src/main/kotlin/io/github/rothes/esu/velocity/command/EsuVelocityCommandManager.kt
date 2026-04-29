package io.github.rothes.esu.velocity.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.esu.velocity.command.parser.UserParser
import io.github.rothes.esu.velocity.config.VelocityEsuLang
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user
import io.github.rothes.esu.velocity.user.ConsoleUser
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.incendo.cloud.*
import org.incendo.cloud.brigadier.BrigadierManagerHolder
import org.incendo.cloud.brigadier.CloudBrigadierCommand
import org.incendo.cloud.brigadier.CloudBrigadierManager
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion
import org.incendo.cloud.caption.CaptionProvider
import org.incendo.cloud.component.CommandComponent
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.internal.CommandRegistrationHandler
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionFactory
import org.incendo.cloud.util.annotation.AnnotationAccessor
import org.incendo.cloud.velocity.VelocityCaptionKeys
import org.incendo.cloud.velocity.VelocityCommandManager
import org.incendo.cloud.velocity.VelocityContextKeys
import org.incendo.cloud.velocity.parser.PlayerParser
import org.incendo.cloud.velocity.parser.ServerParser

class EsuVelocityCommandManager : CommandManager<User>(
    ExecutionCoordinator.asyncCoordinator(),
    EsuVelocityPluginRegistrationHandler(),
),
    BrigadierManagerHolder<User, CommandSource>,
    SenderMapperHolder<CommandSource, User>
{

    private val senderMapper = SenderMapper.create<CommandSource, User>(
        {
            when (it) {
                is ConsoleCommandSource -> ConsoleUser
                is Player               -> it.user
                else                    -> throw IllegalArgumentException("Unsupported user type: ${it.javaClass.name}")
            }
        },
        { it.commandSender as CommandSource }
    )
    private val suggestionFactory = super.suggestionFactory().mapped(TooltipSuggestion::tooltipSuggestion)

    init {
        // VelocityCommandManager
        (commandRegistrationHandler() as EsuVelocityPluginRegistrationHandler).init(this)

        registerCommandPreProcessor { context ->
            context.commandContext().store<ProxyServer>(
                VelocityContextKeys.PROXY_SERVER_KEY, plugin.server
            )
        }

        parserRegistry()
            .registerParser(PlayerParser.playerParser())
            .registerParser(ServerParser.serverParser())

        captionRegistry()
            .registerProvider(
                CaptionProvider.constantProvider<User>()
                .putCaption(VelocityCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER, VelocityCommandManager.ARGUMENT_PARSE_FAILURE_PLAYER)
                .putCaption(VelocityCaptionKeys.ARGUMENT_PARSE_FAILURE_SERVER, VelocityCommandManager.ARGUMENT_PARSE_FAILURE_SERVER)
                .build()
            )

        plugin.server.eventManager.register(plugin.bootstrap, ServerPreConnectEvent::class.java) {
            lockRegistration()
        }

        this.parameterInjectorRegistry().registerInjector(
            CommandSource::class.java
        ) { context: CommandContext<User>, _: AnnotationAccessor ->
            this.senderMapper.reverse(context.sender())
        }

        this.registerDefaultExceptionHandlers(
            { triplet ->
                val source = triplet.first().inject(CommandSource::class.java).orElseThrow { NullPointerException() }
                val message = triplet.first().formatCaption(triplet.second(), triplet.third())
                source.sendMessage(Component.text(message, NamedTextColor.RED))
            },
            { pair -> pair.second().printStackTrace() }
        )

        // Our logic
        registerCapability(CloudCapability.StandardCapabilities.ROOT_COMMAND_DELETION)
        settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true)
        captionRegistry().registerProvider { caption, recipient ->
            recipient.localedOrNull(VelocityEsuLang.get()) {
                commandCaptions[caption]
            }
        }
        parserRegistry().registerParser(UserParser.parser())
        parserRegistry().registerNamedParser("greedyString", StringParser.greedyStringParser())
        EsuExceptionHandlers(exceptionController()).register()

        // Support non-standard username
        brigadierManager().registerMapping(
            object : TypeToken<UserParser<User>>() {}
        ) { builder ->
            builder.cloudSuggestions().toConstant(StringArgumentType.greedyString())
        }
        brigadierManager().registerMapping(
            object : TypeToken<PlayerParser<User>>() {}
        ) { builder ->
            builder.cloudSuggestions().toConstant(StringArgumentType.greedyString())
        }
    }

    override fun hasPermission(sender: User, permission: String): Boolean {
        return this.senderMapper.reverse(sender).hasPermission(permission)
    }

    override fun hasBrigadierManager(): Boolean = true

    override fun brigadierManager(): CloudBrigadierManager<User, CommandSource> {
        return (this.commandRegistrationHandler() as EsuVelocityPluginRegistrationHandler).brigadierManager
    }

    override fun suggestionFactory(): SuggestionFactory<User, out Suggestion> {
        return this.suggestionFactory
    }

    override fun senderMapper(): SenderMapper<CommandSource, User> {
        return this.senderMapper
    }

    private class EsuVelocityPluginRegistrationHandler : CommandRegistrationHandler<User> {

        private var manager: EsuVelocityCommandManager by InitOnce()
        var brigadierManager: CloudBrigadierManager<User, CommandSource> by InitOnce()

        fun init(manager: EsuVelocityCommandManager) {
            this.manager = manager
            this.brigadierManager = CloudBrigadierManager(
                manager,
                manager.senderMapper()
            )
        }

        override fun registerCommand(command: Command<User>): Boolean {
            val component = command.rootComponent()
            val aliases = component.alternativeAliases()
            val brigadierCommand = BrigadierCommand(
                brigadierManager.literalBrigadierNodeFactory().createNode(
                    command.rootComponent().name(),
                    command,
                    CloudBrigadierCommand(manager, brigadierManager)
                )
            )
            val commandMeta = plugin.server.commandManager.metaBuilder(brigadierCommand)
                .aliases(*aliases.toTypedArray())
                .build()
            for (alias in aliases) {
                plugin.server.commandManager.unregister(alias)
            }
            plugin.server.commandManager.register(commandMeta, brigadierCommand)
            return true
        }

        override fun unregisterRootCommand(rootCommand: CommandComponent<User>) {
            for (alias in rootCommand.aliases()) {
                plugin.server.commandManager.unregister(alias)
            }
            plugin.server.commandManager.unregister(rootCommand.name())
        }

    }

}