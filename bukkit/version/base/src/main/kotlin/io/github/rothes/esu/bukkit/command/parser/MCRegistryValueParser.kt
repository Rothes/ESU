package io.github.rothes.esu.bukkit.command.parser

import io.github.rothes.esu.bukkit.util.version.adapter.nms.MCRegistryAccessHandler
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider

class MCRegistryValueParser<C, T>(
    val accessHandler: MCRegistryAccessHandler,
    registryKey: ResourceKey<out Registry<T>>,
): ArgumentParser<C, T>, BlockingSuggestionProvider.Strings<C> {

    val registry = accessHandler.getRegistryOrThrow(accessHandler.getServerRegistryAccess(), registryKey)

    override fun parse(commandContext: CommandContext<C & Any>, commandInput: CommandInput): ArgumentParseResult<T & Any> {
        val input = commandInput.readString()

        val key = ResourceLocation.tryParse(input.lowercase()) ?: return ArgumentParseResult.failure(unknownKey(input))
        val value = accessHandler.get(registry, key) ?: return ArgumentParseResult.failure(unknownKey(input))
        return ArgumentParseResult.success(value)
    }

    override fun stringSuggestions(commandContext: CommandContext<C?>, input: CommandInput): Iterable<String> {
        return accessHandler.entrySet(registry).map { entry ->
            val key = entry.key
            if ((key.location().namespace == ResourceLocation.DEFAULT_NAMESPACE)) key.location().path
            else key.location().toString()
        }
    }

    private fun unknownKey(input: String) = IllegalArgumentException("Unknown key: $input")

}