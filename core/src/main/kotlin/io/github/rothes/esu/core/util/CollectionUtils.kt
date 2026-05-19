/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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
        while (iterator.hasNext()) {
            if (predicate(iterator.nextLong()))
                iterator.remove()
            else
                break
        }
    }
    inline fun IntIterable.removeWhile(predicate: (Int) -> Boolean) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (predicate(iterator.nextInt()))
                iterator.remove()
            else
                break
        }
    }

}