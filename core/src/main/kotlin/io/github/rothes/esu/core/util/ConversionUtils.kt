package io.github.rothes.esu.core.util

import kotlinx.datetime.*

object ConversionUtils {

    val Long.localDateTime
        get() = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val LocalDateTime.epochMilli
        get() = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

}