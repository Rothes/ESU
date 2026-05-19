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