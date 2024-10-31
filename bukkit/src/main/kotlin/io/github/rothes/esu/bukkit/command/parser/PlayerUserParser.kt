package io.github.rothes.esu.bukkit.command.parser

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
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

class PlayerUserParser<C>: ArgumentParser<C, PlayerUser>, BlockingSuggestionProvider<C> {

    override fun parse(
        commandContext: CommandContext<C & Any>, commandInput: CommandInput
    ): ArgumentParseResult<PlayerUser> {
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

        inline fun <reified C> parser(): ParserDescriptor<C, PlayerUser> {
            return ParserDescriptor.of(PlayerUserParser(), PlayerUser::class.java)
        }

    }
}