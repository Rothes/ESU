package io.github.rothes.esu.core.util.extension

fun <T> listOfJvm(vararg elements: T): List<T> {
    return mutableListOf<T>().apply {
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