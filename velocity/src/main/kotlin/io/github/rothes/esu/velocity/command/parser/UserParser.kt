/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.velocity.command.parser

import com.velocitypowered.api.command.VelocityBrigadierMessage
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user
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

class UserParser<C>: ArgumentParser<C, User>, BlockingSuggestionProvider<C> {

    override fun parse(
        commandContext: CommandContext<C & Any>, commandInput: CommandInput
    ): ArgumentParseResult<User> {
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

        inline fun <reified C> parser(): ParserDescriptor<C, User> {
            return ParserDescriptor.of(UserParser(), User::class.java)
        }

    }

    class UserParseException(input: String, context: CommandContext<*>) : ParserException(
        PlayerParser::class.java, context, VelocityCaptionKeys.ARGUMENT_PARSE_FAILURE_PLAYER,
        CaptionVariable.of("input", input)
    )

}