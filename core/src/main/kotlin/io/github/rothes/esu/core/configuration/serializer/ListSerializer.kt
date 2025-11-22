package io.github.rothes.esu.core.configuration.serializer

import java.lang.reflect.AnnotatedType

object ListSerializer: AbstractListSerializer<List<*>>() {

    override fun createNew(length: Int, elementType: AnnotatedType?): List<Any?> {
        return ArrayList(length)
    }

}