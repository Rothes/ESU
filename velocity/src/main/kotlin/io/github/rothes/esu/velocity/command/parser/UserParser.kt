package io.github.rothes.esu.velocity.command.parser

import com.velocitypowered.api.command.VelocityBrigadierMessage
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user
import io.github.rothes.esu.velocity.user.VelocityUser
import net.kyori.adventure.text.Component
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion
import org.incendo.cloud.caption.CaptionVariable
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.exception.parsing.ParserException
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.velocity.VelocityCaptionKeys
import org.incendo.cloud.velocity.parser.PlayerParser
import kotlin.jvm.optionals.getOrNull

class UserParser<C>: ArgumentParser<C, VelocityUser>, BlockingSuggestionProvider<C> {

    override fun parse(
        commandContext: CommandContext<C & Any>, commandInput: CommandInput
    ): ArgumentParseResult<VelocityUser> {
        val input = commandInput.readString()

        val player = plugin.server.getPlayer(input).getOrNull()
            ?: return ArgumentParseResult.failure(UserParseException(input, commandContext))

        return ArgumentParseResult.success(player.user)
    }

    override fun suggestions(context: CommandContext<C>, input: CommandInput): MutableIterable<Suggestion> {
        return plugin.server.allPlayers.map {
            TooltipSuggestion.suggestion(
                it!!.username,
                VelocityBrigadierMessage.tooltip(Component.text(it.uniqueId.toString()))
            )
        }.toMutableList()
    }

    companion object {

        inline fun <reified C> parser(): ParserDescriptor<C, VelocityUser> {
            return ParserDescriptor.of(UserParser(), VelocityUser::class.java)
        }

    }

    class UserParseException(input: String, context: CommandContext<*>) : ParserException(
        PlayerParser::class.java, context, VelocityCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER,
        CaptionVariable.of("input", input)
    )

}