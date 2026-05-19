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

package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Predicate
import java.util.regex.Matcher

object PathSerializer: ScalarSerializer<Path>(Path::class.java) {

    override fun deserialize(type: Type, obj: Any): Path {
        val string = obj.toString()
        return Paths.get(string.replaceFirst("^~/?".toRegex(), Matcher.quoteReplacement(System.getProperty("user.home") + '/')))
    }

    override fun serialize(path: Path, typeSupported: Predicate<Class<*>>): String {
        return path.toAbsolutePath().toString()
    }

}
