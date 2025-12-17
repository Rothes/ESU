package io.github.rothes.esu.core.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object ConversionUtils {

    val Long.localDateTime
        get() = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    @OptIn(ExperimentalTime::class)
    val LocalDateTime.epochMilli
        get() = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

}