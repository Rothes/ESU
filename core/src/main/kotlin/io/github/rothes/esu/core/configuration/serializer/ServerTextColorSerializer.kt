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

package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import java.lang.reflect.Type
import java.util.function.Predicate

object ServerTextColorSerializer: ScalarSerializer<TextColor>(TextColor::class.java) {

    private val aliases = mapOf(Pair("dark_grey", NamedTextColor.DARK_GRAY), Pair("grey", NamedTextColor.GRAY))

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): TextColor {
        val string = obj.toString()
        return (
                if (string.startsWith("#")) TextColor.fromHexString(string)
                else NamedTextColor.NAMES.value(string) ?: aliases[string]
                ) ?: throw SerializationException("Unable to parse a color from '$string'. Please use named colors or hex (#RRGGBB) colors.")
    }

    override fun serialize(textColor: TextColor, typeSupported: Predicate<Class<*>>): String {
        return textColor.toString()
    }

}
