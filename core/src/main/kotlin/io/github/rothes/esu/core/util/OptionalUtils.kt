/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.core.util

import java.util.*

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