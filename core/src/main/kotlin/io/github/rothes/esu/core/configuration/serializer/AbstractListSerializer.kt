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

import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.lib.configurate.ConfigurationOptions
import io.github.rothes.esu.lib.configurate.serialize.AbstractListChildSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import io.github.rothes.esu.lib.configurate.util.CheckedConsumer
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType

abstract class AbstractListSerializer<T>: AbstractListChildSerializer<T>() {

    override fun emptyValue(specificType: AnnotatedType, options: ConfigurationOptions): T? {
        if (specificType.isAnnotationPresent(NoDeserializeNull::class.java)) {
            return null
        }
        return super.emptyValue(specificType, options)
    }

    override fun deserializeSingle(index: Int, collection: T, deserialized: Any?) {
        @Suppress("UNCHECKED_CAST") (collection as MutableCollection<Any?>).add(deserialized)
    }

    override fun forEachElement(collection: T, action: CheckedConsumer<Any, SerializationException>) {
        @Suppress("UNCHECKED_CAST") (collection as Iterable<Any?>).forEach { action.accept(it) }
    }

    override fun elementType(containerType: AnnotatedType?): AnnotatedType {
        if (containerType !is AnnotatedParameterizedType) {
            throw SerializationException(containerType, "Raw types are not supported for collections")
        }
        return containerType.annotatedActualTypeArguments[0]
    }

}