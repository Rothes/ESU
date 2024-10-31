package io.github.rothes.esu.core.command.parser

import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.parser.standard.StringParser.StringParseException
import org.incendo.cloud.suggestion.BlockingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion

class ModuleParser<C>: ArgumentParser<C, Module<*, *>>, BlockingSuggestionProvider<C> {

    override fun parse(
        commandContext: CommandContext<C & Any>, commandInput: CommandInput
    ): ArgumentParseResult<Module<*, *>> {
        val input = commandInput.readString()

        val module =  ModuleManager[input]
            ?: return ArgumentParseResult.failure(StringParseException(input, StringParser.StringMode.SINGLE, commandContext))

        return ArgumentParseResult.success(module)
    }

    override fun suggestions(context: CommandContext<C>, input: CommandInput): MutableIterable<Suggestion> {
        return ModuleManager.registeredModules().map { Suggestion.suggestion(it.name) }.toMutableList()
    }

    companion object {

        inline fun <reified C> parser(): ParserDescriptor<C, Module<*, *>> {
            return ParserDescriptor.of(ModuleParser(), Module::class.java)
        }

    }
}