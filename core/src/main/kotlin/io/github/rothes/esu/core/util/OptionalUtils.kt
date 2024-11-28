package io.github.rothes.esu.core.util

import java.util.Optional

object OptionalUtils {

    /**
     * Handle the obj passed with func if a value is present, and return the new obj. Else, return the original obj.
     */
    fun <T, R> Optional<T>.applyTo(obj: R, func: (T) -> R): R {
        if (this.isPresent) {
            return get().let(func)
        }
        return obj
    }

    fun <T, R> R.optional(optional: Optional<T>, func: R.(T) -> R): R {
        if (optional.isPresent) {
            return func(optional.get())
        }
        return this
    }

}