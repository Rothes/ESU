@file:Suppress("NOTHING_TO_INLINE")

package io.github.rothes.esu.core.util.extension.math

import kotlin.math.floor

fun Int.square(): Int = this * this
fun Long.square(): Long = this * this
fun Float.square(): Float = this * this
fun Double.square(): Double = this * this

fun Float.floorI(): Int = floor(this.toDouble()).toInt()
fun Double.floorI(): Int = floor(this).toInt()

fun Double.frac(): Double = this - floor(this)