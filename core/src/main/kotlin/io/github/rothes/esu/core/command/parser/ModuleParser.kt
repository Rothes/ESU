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

package io.github.rothes.esu.core.command.parser

import io.github.rothes.esu.core.module.Module
import io.github.rothes.esu.core.module.ModuleManager
import io.leangen.geantyref.TypeToken
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
            return ParserDescriptor.of(ModuleParser(), object : TypeToken<Module<*, *>>() {})
        }

    }
}