package io.github.rothes.esu.core.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class VarLazy<T, V>(val initializer: () -> V): ReadWriteProperty<T, V> {

    private var value: V? = null

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return value ?: initializer().also { value = it }
    }

    override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
        this.value = value
    }

}