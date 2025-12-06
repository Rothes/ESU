package io.github.rothes.esu.core.util.extension

fun String.charSize(): Int = sumOf { if (it.code <= 591) 1 else 2 }