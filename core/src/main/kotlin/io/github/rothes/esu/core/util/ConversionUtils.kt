package io.github.rothes.esu.core.util

import kotlinx.datetime.*
import kotlin.time.ExperimentalTime

object ConversionUtils {

    val Long.localDateTime
        get() = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    @OptIn(ExperimentalTime::class)
    val LocalDateTime.epochMilli
        get() = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

}