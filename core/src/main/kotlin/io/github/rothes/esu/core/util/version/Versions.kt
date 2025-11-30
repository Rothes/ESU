package io.github.rothes.esu.core.util.version

fun String.toVersion(): Version = Version.fromString(this)