package io.github.rothes.esu.core.util.extension

import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.longs.LongList

fun <T> listOfJvm(element: T): List<T> {
    return ArrayList<T>(1).apply {
        add(element)
    }
}

fun <T> listOfJvm(vararg elements: T): List<T> {
    return ArrayList<T>(elements.size).apply {
        elements.forEach {
            add(it)
        }
    }
}

inline fun <T, R> Collection<T>.mapJvm(transform: (T) -> R): List<R> {
    val destination = ArrayList<R>(size)
    for (item in this)
        destination.add(transform(item))
    return destination
}

inline fun IntList.forEachInt(action: (Int) -> Unit) {
    for (i in 0 until this.size)
        action(getInt(i))
}

inline fun LongList.forEachLong(action: (Long) -> Unit) {
    for (i in 0 until this.size)
        action(getLong(i))
}