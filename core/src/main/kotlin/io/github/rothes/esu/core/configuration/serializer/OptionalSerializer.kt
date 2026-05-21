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

import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.lib.configurate.ConfigurationNode
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import io.github.rothes.esu.lib.configurate.serialize.TypeSerializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

object OptionalSerializer: TypeSerializer<Optional<*>> {

    const val DISABLED = "disabled"

    override fun deserialize(type: Type, node: ConfigurationNode): Optional<*> {
        if (type !is ParameterizedType) {
            throw SerializationException(type, "Raw types are not supported for optional")
        }
        if (type.actualTypeArguments.size != 1) {
            throw SerializationException(type, "Optional expected a type argument!")
        }
        val value = type.actualTypeArguments[0]
        val serializer = node.options().serializers()[value] ?: throw SerializationException(
            type, "No type serializer available for optional type $value"
        )
        if (node.raw() == DISABLED) {
            return Optional.empty<Any>()
        }
        try {
            return Optional.ofNullable(serializer.deserialize(value, node))
        } catch (ex: SerializationException) {
            ex.initPath { node.path() }
            EsuCore.instance.err("Could not deserialize ${node.raw()} into $type at ${node.path()}: ${ex.rawMessage()}")
            return Optional.empty<Any>()
        }
    }

    override fun serialize(type: Type, obj: Optional<*>?, node: ConfigurationNode) {
        if (type !is ParameterizedType) {
            throw SerializationException(type, "Raw types are not supported for optional")
        }
        if (type.actualTypeArguments.size != 1) {
            throw SerializationException(type, "Optional expected a type argument!")
        }
        if (obj == null || obj.isEmpty) {
            node.set(DISABLED)
        } else {
            val value = type.actualTypeArguments[0]
            val serializer = node.options().serializers()[value] ?: throw SerializationException(
                type, "No type serializer available for optional type $value"
            )
            @Suppress("UNCHECKED_CAST")
            (serializer as TypeSerializer<Any>).serialize(value, obj.get(), node)
        }
    }

}