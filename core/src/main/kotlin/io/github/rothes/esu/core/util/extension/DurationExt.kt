package io.github.rothes.esu.core.util.extension

import java.time.Duration

object DurationExt {

    val Duration.valuePositive: Boolean
        get() = (seconds and nano.toLong()) > 0
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