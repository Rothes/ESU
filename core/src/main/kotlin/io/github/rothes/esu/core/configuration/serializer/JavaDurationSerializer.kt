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
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.time.Duration
import java.util.function.Predicate
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

object JavaDurationSerializer: ScalarSerializer<Duration>(Duration::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Duration {
        when (obj) {
            is Long -> return Duration.ofMillis(obj)
            is Int -> return Duration.ofMillis(obj.toLong())
        }
        val string = obj.toString()
        return kotlin.time.Duration.parse(string).toJavaDuration()
    }

    override fun serialize(duration: Duration, typeSupported: Predicate<Class<*>>): String {
        return duration.toKotlinDuration().toString()
    }

}
