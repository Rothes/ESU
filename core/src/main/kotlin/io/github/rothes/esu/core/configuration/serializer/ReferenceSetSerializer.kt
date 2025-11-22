package io.github.rothes.esu.core.configuration.serializer

import it.unimi.dsi.fastutil.objects.ReferenceArraySet
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet
import it.unimi.dsi.fastutil.objects.ReferenceSet
import java.lang.reflect.AnnotatedType

object ReferenceSetSerializer: AbstractListSerializer<ReferenceSet<*>>() {

    override fun createNew(length: Int, elementType: AnnotatedType?): ReferenceSet<Any> {
        return if (length <= 3) ReferenceArraySet(length) else ReferenceLinkedOpenHashSet(length)
    }

}