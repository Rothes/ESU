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

package io.github.rothes.esu.core.module.configuration

import io.github.rothes.esu.lib.configurate.ConfigurationOptions
import io.github.rothes.esu.lib.configurate.serialize.ScalarSerializer
import java.lang.reflect.Type
import java.util.function.Predicate

abstract class FeatureToggle(
    override val enabled: Boolean,
) : EnableTogglable {

    class DefaultTrue private constructor(value: Boolean = true): FeatureToggle(value) {
        companion object {
            val TRUE = DefaultTrue(true)
            val FALSE = DefaultTrue(false)

            val SERIALIZER = Serializer<DefaultTrue>(DefaultTrue::class.java, TRUE, TRUE, FALSE)
        }
    }
    class DefaultFalse private constructor(value: Boolean = false): FeatureToggle(value) {
        companion object {
            val TRUE = DefaultFalse(true)
            val FALSE = DefaultFalse(false)

            val SERIALIZER = Serializer<DefaultFalse>(DefaultFalse::class.java, FALSE, TRUE, FALSE)
        }
    }

    class Serializer<T: FeatureToggle>(
        val type: Class<T>,
        val default: T,
        val t: T,
        val f: T,
    ): ScalarSerializer<T>(type) {

        override fun deserialize(type: Type?, obj: Any?): T {
            return if (obj as? Boolean ?: obj.toString().toBoolean()) t else f
        }

        override fun serialize(item: T, typeSupported: Predicate<Class<*>?>): Any {
            return item.enabled
        }

        override fun emptyValue(specificType: Type?, options: ConfigurationOptions?): T {
            return default
        }

    }

}