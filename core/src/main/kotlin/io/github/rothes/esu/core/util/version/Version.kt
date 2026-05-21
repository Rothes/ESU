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

package io.github.rothes.esu.core.util.version

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        return when {
            major != other.major -> major - other.major
            minor != other.minor -> minor - other.minor
            else -> patch - other.patch
        }
    }

    operator fun compareTo(other: String): Int = this.compareTo(fromString(other))

    operator fun compareTo(major: Int): Int = this.major - major

    operator fun plus(version: Version): Version {
        return Version(
            major + version.major,
            minor + version.minor,
            patch + version.patch
        )
    }

    fun shortString(): String {
        return buildString {
            append(major)
            if (minor != 0 || patch != 0) {
                append('.')
                append(minor)
            }
            if (patch != 0) {
                append('.')
                append(patch)
            }
        }
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    companion object {
        fun fromString(versionString: String): Version {
            val parts = versionString.split('.').map { it.toInt() }
            require(parts.size <= 3) { "To many versions passed to version string: $versionString" }
            return Version(parts.getOrElse(0) { 0 }, parts.getOrElse(1) { 0 }, parts.getOrElse(2) { 0 })
        }

    }

}
