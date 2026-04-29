package io.github.rothes.esu.velocity.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.ConsoleCommandSource
import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.core.command.EsuExceptionHandlers
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.velocity.command.parser.UserParser
import io.github.rothes.esu.velocity.config.VelocityEsuLang
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user
import io.github.rothes.esu.velocity.user.ConsoleUser
import io.leangen.geantyref.TypeToken
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.setting.ManagerSetting
import org.incendo.cloud.velocity.VelocityCommandManager
import org.incendo.cloud.velocity.parser.PlayerParser

class EsuVelocityCommandManager : VelocityCommandManager<User>(
    plugin.container,
    plugin.server,
    ExecutionCoordinator.asyncCoordinator(),
    SenderMapper.create(
        {
            when (it) {
                is ConsoleCommandSource -> ConsoleUser
                is Player               -> it.user
                else                    -> throw IllegalArgumentException("Unsupported user type: ${it.javaClass.name}")
            }
        },
        { it.commandSender as CommandSource }
    )
) {
    init {
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

}