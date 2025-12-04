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