package io.github.rothes.esu.bungee.command.parser

import io.github.rothes.esu.bungee.user
import io.github.rothes.esu.bungee.user.BungeeUser
import net.md_5.bungee.api.ProxyServer
import org.incendo.cloud.bungee.BungeeCaptionKeys
import org.incendo.cloud.bungee.parser.PlayerParser
import org.incendo.cloud.caption.CaptionVariable
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.exception.parsing.ParserException
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion

class UserParser<C>: ArgumentParser<C, BungeeUser>, BlockingSuggestionProvider<C> {

    override fun parse(
        commandContext: CommandContext<C & Any>, commandInput: CommandInput
    ): ArgumentParseResult<BungeeUser> {
        val input = commandInput.readString()

        val player = ProxyServer.getInstance().getPlayer(input)
            ?: return ArgumentParseResult.failure(UserParseException(input, commandContext))

        return ArgumentParseResult.success(player.user)
    }

    override fun suggestions(context: CommandContext<C>, input: CommandInput): MutableIterable<Suggestion> {
        return ProxyServer.getInstance().players.map {
            Suggestion.suggestion(it.name)
        }.toMutableList()
    }

    companion object {

        inline fun <reified C> parser(): ParserDescriptor<C, BungeeUser> {
            return ParserDescriptor.of(UserParser(), BungeeUser::class.java)
        }

    }

    class UserParseException(input: String, context: CommandContext<*>) : ParserException(
        PlayerParser::class.java, context, BungeeCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER,
        CaptionVariable.of("input", input)
    )

}