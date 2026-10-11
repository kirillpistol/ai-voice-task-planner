package dem.dev.timeflame.util.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun LocalDateTime.Companion.now(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
    return Clock.System.now().toLocalDateTime(timeZone)
}

fun LocalDateTime.timestamp(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    return this.toInstant(timeZone).toEpochMilliseconds()
}

fun LocalDateTime.convertToZone(
    prevZone: TimeZone = TimeZone.currentSystemDefault(),
    toZone: TimeZone = TimeZone.UTC
): LocalDateTime {
    return this.timestamp(prevZone).toLocalDateTime(toZone)
}

fun Long.toLocalDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
    // convert timestamp to LocalDateTime
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)
}

fun LocalDateTime.copy(
    year: Int = this.year,
    month: Month = this.month,
    dayOfMonth: Int = this.dayOfMonth,
    hour: Int = this.hour,
    minute: Int = this.minute,
    second: Int = this.second,
    nanosecond: Int = this.nanosecond
): LocalDateTime {
    return LocalDateTime(
        year = year,
        month = month,
        dayOfMonth = dayOfMonth,
        hour = hour,
        minute = minute,
        second = second,
        nanosecond = nanosecond
    )
}