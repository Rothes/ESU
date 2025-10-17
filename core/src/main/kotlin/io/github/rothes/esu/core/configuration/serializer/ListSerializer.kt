package io.github.rothes.esu.core.configuration.serializer

import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.lib.configurate.ConfigurationOptions
import io.github.rothes.esu.lib.configurate.serialize.AbstractListChildSerializer
import io.github.rothes.esu.lib.configurate.serialize.SerializationException
import io.github.rothes.esu.lib.configurate.util.CheckedConsumer
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType

object ListSerializer: AbstractListChildSerializer<List<*>>() {

    override fun emptyValue(specificType: AnnotatedType, options: ConfigurationOptions): List<*>? {
        if (specificType.isAnnotationPresent(NoDeserializeNull::class.java)) {
            return null
        }
        return super.emptyValue(specificType, options)
    }

    override fun deserializeSingle(index: Int, collection: List<*>, deserialized: Any?) {
        @Suppress("UNCHECKED_CAST") (collection as MutableList<Any?>).add(deserialized)
    }

    override fun forEachElement(collection: List<*>, action: CheckedConsumer<Any, SerializationException>) {
        collection.forEach { action.accept(it) }
    }

    override fun elementType(containerType: AnnotatedType?): AnnotatedType {
        if (containerType !is AnnotatedParameterizedType) {
            throw SerializationException(containerType, "Raw types are not supported for collections")
        }
        return containerType.annotatedActualTypeArguments[0]
    }

    override fun createNew(length: Int, elementType: AnnotatedType?): List<*> {
        return arrayListOf<Any>()
    }

}