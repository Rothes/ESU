/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.command.parser

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.core.user.User
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.incendo.cloud.bukkit.BukkitCommandContextKeys
import org.incendo.cloud.bukkit.parser.PlayerParser.PlayerParseException
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion

class UserParser<C>: ArgumentParser<C, User>, BlockingSuggestionProvider<C> {

    override fun parse(
        commandContext: CommandContext<C & Any>, commandInput: CommandInput
    ): ArgumentParseResult<User> {
        val input = commandInput.readString()

        val player = Bukkit.getPlayer(input)
            ?: return ArgumentParseResult.failure(PlayerParseException(input, commandContext))

        return ArgumentParseResult.success(player.user)
    }

    override fun suggestions(context: CommandContext<C>, input: CommandInput): MutableIterable<Suggestion> {
        val bukkit: CommandSender = context.get<CommandSender>(BukkitCommandContextKeys.BUKKIT_COMMAND_SENDER)
        return Bukkit.getOnlinePlayers()
            .filter { it.isOnline && (bukkit !is Player || bukkit.canSee(it)) }
            .mapNotNull { Suggestion.suggestion(it.name) }.toMutableList()
    }

    companion object {

        inline fun <reified C> parser(): ParserDescriptor<C, User> {
            return ParserDescriptor.of(UserParser(), User::class.java)
        }

    }
}