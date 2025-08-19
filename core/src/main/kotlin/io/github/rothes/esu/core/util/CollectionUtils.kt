package io.github.rothes.esu.core.util

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

}