package io.github.rothes.esu.core.util

import it.unimi.dsi.fastutil.ints.IntIterable
import it.unimi.dsi.fastutil.longs.LongIterable

object CollectionUtils {

    fun <T> Iterable<T>.randomWeighted(weight: (T) -> Int): T {
        val weights = this.sumOf(weight)
        val random = (0 until weights).random()
        var i = 0
        return this.first { item ->
            i += weight(item)
            i > random
        }
    }

    inline fun <T> MutableIterable<T>.removeWhile(predicate: (T) -> Boolean) {
        val iterator = iterator()
        for (element in iterator) {
            if (predicate(element)) {
                iterator.remove()
            } else {
                break
            }
        }
    }

    inline fun LongIterable.removeWhile(predicate: (Long) -> Boolean) {
        val iterator = iterator()
        for (element in iterator) {
            if (predicate(element)) {
                iterator.remove()
            } else {
                break
            }
        }
    }
    inline fun IntIterable.removeWhile(predicate: (Int) -> Boolean) {
        val iterator = iterator()
        for (element in iterator) {
            if (predicate(element)) {
                iterator.remove()
            } else {
                break
            }
        }
    }

}