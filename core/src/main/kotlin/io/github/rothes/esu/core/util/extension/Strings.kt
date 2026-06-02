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

package io.github.rothes.esu.core.util.extension

fun String.charSize(): Int = sumOf { if (it.code <= 591) 1 else 2 }

fun String.substringCharSize(start: Int, end: Int): String {
    var head = -1
    var ptr = 0
    var size = 0
    while (ptr < length) {
        size += if (this[ptr].code <= 591) 1 else 2
        if (head == -1 && size >= start) {
            head = ptr
        }
        if (size > end)
            return substring(head, ptr)
        else if (size == end)
            return substring(head, ptr + 1)
        ptr++
    }
    throw IndexOutOfBoundsException()
}