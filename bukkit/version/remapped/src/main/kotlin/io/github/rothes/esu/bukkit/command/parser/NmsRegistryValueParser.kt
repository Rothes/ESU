package io.github.rothes.esu.bukkit.command.parser

import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ResourceKeyHandler
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.suggestion.BlockingSuggestionProvider

class NmsRegistryValueParser<C, T: Any>(
    val accessHandler: NmsRegistryAccessHandler,
    registryKey: ResourceKey<out Registry<T>>,
): ArgumentParser<C, T>, BlockingSuggestionProvider.Strings<C> {

    companion object {
        private val KEY_HANDLER by Versioned(ResourceKeyHandler::class.java)
    }

    val registry = accessHandler.getRegistryOrThrow(registryKey)

    override fun parse(commandContext: CommandContext<C & Any>, commandInput: CommandInput): ArgumentParseResult<T> {
        val input = commandInput.readString()

        val key = try {
            KEY_HANDLER.parseResourceKey(registry, input.lowercase())
        } catch (_: ResourceKeyHandler.BadIdentifierException) {
            return ArgumentParseResult.failure(unknownKey(input))
        }
        val value = accessHandler.getNullable(registry, key) ?: return ArgumentParseResult.failure(unknownKey(input))
        return ArgumentParseResult.success(value)
    }

    override fun stringSuggestions(commandContext: CommandContext<C?>, input: CommandInput): Iterable<String> {
        return accessHandler.entrySet(registry).map { entry ->
            val key = entry.key
            KEY_HANDLER.getResourceKeyString(key)
        }
    }

    private fun unknownKey(input: String) = IllegalArgumentException("Unknown key: $input")

}