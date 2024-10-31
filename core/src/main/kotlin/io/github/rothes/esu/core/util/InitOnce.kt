package io.github.rothes.esu.core.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class InitOnce<T, V>: ReadWriteProperty<T, V> {

    private var value: V? = null

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return value ?: throw IllegalStateException("${property.name} has not been initialized")
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        if (this.value != null) {
            throw IllegalStateException("${property.name} is already initialized")
        }
        this.value = value
    }

}