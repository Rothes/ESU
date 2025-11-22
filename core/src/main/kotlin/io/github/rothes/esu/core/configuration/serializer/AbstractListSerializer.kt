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