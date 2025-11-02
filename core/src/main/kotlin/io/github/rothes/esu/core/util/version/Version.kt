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

    operator fun plus(version: Version): Version {
        return Version(
            major + version.major,
            minor + version.minor,
            patch + version.patch
        )
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

        fun String.toVersion(): Version = fromString(this)
    }

}
