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

import java.time.Duration

object DurationExt {

    val Duration.valuePositive: Boolean
        get() = (seconds or nano.toLong()) > 0
    val Duration.valueNegative: Boolean
        get() = seconds < 0

    operator fun Duration.compareTo(duration: Long): Int {
        return toMillis().compareTo(duration)
    }
    operator fun Long.compareTo(duration: Duration): Int {
        return compareTo(duration.toMillis())
    }

    operator fun Duration.compareTo(duration: Double): Int {
        return toMillis().compareTo(duration)
    }
    operator fun Double.compareTo(duration: Duration): Int {
        return compareTo(duration.toMillis())
    }

}