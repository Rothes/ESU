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

package io.github.rothes.esu.core.util.version

fun String.toVersion(): Version = Version.fromString(this)

fun Version.drop(n: Int): Version {
    require(n >= 0) { "Drop count cannot be negative: $n" }
    return when (n) {
        0 -> copy()
        1 -> Version(major = minor, minor = patch, patch = 0)
        2 -> Version(major = patch, minor = 0, patch = 0)
        else -> Version(0, 0, 0)
    }
}