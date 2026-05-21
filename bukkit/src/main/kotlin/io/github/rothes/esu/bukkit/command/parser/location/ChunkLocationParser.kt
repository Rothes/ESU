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

package io.github.rothes.esu.bukkit.command.parser.location

import io.github.rothes.esu.core.util.ReflectionUtils.handle
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.BlockCommandSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import org.incendo.cloud.bukkit.BukkitCaptionKeys
import org.incendo.cloud.bukkit.BukkitCommandContextKeys
import org.incendo.cloud.bukkit.parser.location.LocationCoordinate
import org.incendo.cloud.bukkit.parser.location.LocationCoordinateParser
import org.incendo.cloud.bukkit.parser.location.LocationCoordinateType
import org.incendo.cloud.bukkit.parser.location.LocationParser
import org.incendo.cloud.caption.Caption
import org.incendo.cloud.caption.CaptionVariable
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.context.CommandInput
import org.incendo.cloud.exception.parsing.ParserException
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.parser.ArgumentParser
import org.incendo.cloud.parser.ParserDescriptor
import org.incendo.cloud.suggestion.BlockingSuggestionProvider

class ChunkLocationParser<C>: ArgumentParser<C, ChunkLocation>, BlockingSuggestionProvider.Strings<C> {

    private val locationCoordinateParser = LocationCoordinateParser<C>()
    private val toLocalSpace = LocationParser::class.java.getDeclaredMethod("toLocalSpace", Location::class.java, Vector::class.java).handle
    private val getSuggestions = LocationParser::class.java.getDeclaredMethod("getSuggestions", Int::class.java, CommandContext::class.java, CommandInput::class.java).handle

    override fun parse(
        commandContext: CommandContext<C & Any>, commandInput: CommandInput
    ): ArgumentParseResult<ChunkLocation> {

        fun wrongFormatException() = ArgumentParseResult.failure<ChunkLocation>(
            LocationParseException(
                commandContext, BukkitCaptionKeys.ARGUMENT_PARSE_FAILURE_LOCATION_INVALID_FORMAT, commandInput.remainingInput()
            )
        )

        if (commandInput.remainingTokens() < 2) {
            return wrongFormatException()
        }
        val coordinates = arrayOfNulls<LocationCoordinate>(2)
        for (i in 0..1) {
            if (commandInput.peekString().isEmpty()) {
                return wrongFormatException()
            }
            val coordinate = this.locationCoordinateParser.parse(commandContext, commandInput)
            if (coordinate.failure().isPresent) {
                return ArgumentParseResult.failure(coordinate.failure().get())
            }
            coordinates[i] = coordinate.parsedValue().orElseThrow { NullPointerException() }
        }
        val bukkitSender = commandContext.get<CommandSender>(BukkitCommandContextKeys.BUKKIT_COMMAND_SENDER)

        val location = when (bukkitSender) {
            is BlockCommandSender -> bukkitSender.block.location
            is Entity             -> bukkitSender.location
            else                  -> Location(Bukkit.getWorlds()[0], 0.0, 0.0, 0.0)
        }
        location.x = (location.blockX shr 4).toDouble()
        location.z = (location.blockZ shr 4).toDouble()

        if (coordinates[0]!!.type() == LocationCoordinateType.LOCAL && coordinates[1]!!.type() != LocationCoordinateType.LOCAL) {
            return ArgumentParseResult.failure(
                LocationParseException(
                    commandContext, BukkitCaptionKeys.ARGUMENT_PARSE_FAILURE_LOCATION_MIXED_LOCAL_ABSOLUTE, ""
                )
            )
        }

        if (coordinates[0]!!.type() == LocationCoordinateType.ABSOLUTE) {
            location.x = coordinates[0]!!.coordinate()
        } else if (coordinates[0]!!.type() == LocationCoordinateType.RELATIVE) {
            location.add(coordinates[0]!!.coordinate(), 0.0, 0.0)
        }

        if (coordinates[1]!!.type() == LocationCoordinateType.ABSOLUTE) {
            location.z = coordinates[1]!!.coordinate()
        } else if (coordinates[1]!!.type() == LocationCoordinateType.RELATIVE) {
            location.add(0.0, 0.0, coordinates[1]!!.coordinate())
        } else {
            val declaredPos = Vector(coordinates[0]!!.coordinate(), 0.0, coordinates[1]!!.coordinate())
            val local = toLocalSpace.invokeExact(location, declaredPos) as Location
            return ArgumentParseResult.success(ChunkLocation(location.world, local.x, local.z))
        }

        return ArgumentParseResult.success(ChunkLocation(location.world, location.x, location.z))
    }

    override fun stringSuggestions(
        commandContext: CommandContext<C?>, input: CommandInput
    ): Iterable<String> {
        @Suppress("UNCHECKED_CAST")
        return getSuggestions.invokeExact(2, commandContext, input) as List<String>
    }


    private class LocationParseException(
        context: CommandContext<*>, caption: Caption, input: String
    ) : ParserException(
        LocationParser::class.java, context, caption, CaptionVariable.of("input", input)
    )

    companion object {

        inline fun <reified C> parser(): ParserDescriptor<C, ChunkLocation> {
            return ParserDescriptor.of(ChunkLocationParser(), ChunkLocation::class.java)
        }

    }
}