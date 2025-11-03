@file:Suppress("NOTHING_TO_INLINE")

package io.github.rothes.esu.core.util.extension.math

import kotlin.math.floor

inline fun Int.square(): Int = this * this
inline fun Long.square(): Long = this * this
inline fun Float.square(): Float = this * this
inline fun Double.square(): Double = this * this

inline fun Float.floorI(): Int = floor(this.toDouble()).toInt()
inline fun Double.floorI(): Int = floor(this).toInt()