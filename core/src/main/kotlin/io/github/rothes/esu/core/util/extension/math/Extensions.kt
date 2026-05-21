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

package io.github.rothes.esu.core.util.extension.math

import kotlin.math.floor

fun square(i: Int): Int = i * i
fun square(l: Long): Long = l * l
fun square(f: Float): Float = f * f
fun square(d: Double): Double = d * d

fun floorI(f: Float): Int = floor(f.toDouble()).toInt()
fun floorI(d: Double): Int = floor(d).toInt()

fun floorL(f: Float): Long = floor(f.toDouble()).toLong()
fun floorL(d: Double): Long = floor(d).toLong()

fun frac(d: Double): Double = d - floor(d)