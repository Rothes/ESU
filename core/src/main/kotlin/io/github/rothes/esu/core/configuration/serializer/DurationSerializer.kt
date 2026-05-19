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
import java.util.function.Predicate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object DurationSerializer: ScalarSerializer<Duration>(Duration::class.java) {

    @Throws(SerializationException::class)
    override fun deserialize(type: Type, obj: Any): Duration {
        when (obj) {
            is Long -> return obj.milliseconds
            is Int -> return obj.milliseconds
        }
        val string = obj.toString()
        return Duration.parse(string)
    }

    override fun serialize(duration: Duration, typeSupported: Predicate<Class<*>>): String {
        return duration.toString()
    }

}
