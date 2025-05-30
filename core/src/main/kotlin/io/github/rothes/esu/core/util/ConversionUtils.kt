package io.github.rothes.esu.core.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object ConversionUtils {

    val Long.localDateTime
        get() = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

}