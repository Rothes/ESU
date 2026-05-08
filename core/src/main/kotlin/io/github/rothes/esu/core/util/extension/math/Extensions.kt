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