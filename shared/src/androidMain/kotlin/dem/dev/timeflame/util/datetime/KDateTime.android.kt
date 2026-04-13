package dem.dev.timeflame.util.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.Instant

actual fun KDateTime.Companion.now(): KDateTime {
    return KDateTime()
}

actual fun KDateTime.Companion.fromTimestamp(timestamp: Long): KDateTime {
    return KDateTime(timestamp.toLocalDateTime())
}

// convert from utc timestamp to local kdatetime
actual fun KDateTime.Companion.fromUtcTimestamp(timestamp: Long): KDateTime {
    val localTimeZoneOffset = localTimeZone.offsetAt(kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)).totalSeconds * 1000 // local timezone offset in millis
    return KDateTime((timestamp + localTimeZoneOffset).toLocalDateTime())
}

actual fun KDateTime.Companion.fromDate(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int
): KDateTime? {
    return KDateTime(LocalDateTime(
        year = year,
        monthNumber = month,
        dayOfMonth = day,
        hour = hour,
        minute = minute,
        second = second
    ))
}

actual class KDateTime(private val date: LocalDateTime = LocalDateTime.now()) {

    actual val monthNumber: Int
        get() = this.date.monthNumber

    actual val year: Int
        get() = this.date.year

    actual val dayOfMonth: Int
        get() = this.date.dayOfMonth

    actual val dayOfWeek: Int
        get() = this.date.dayOfWeek.isoDayNumber

    actual fun timestamp(): Long {
        return date.timestamp()
    }

    actual fun utcTimestamp(): Long {
        val localTimeZoneOffset = localTimeZone.offsetAt(kotlinx.datetime.Instant.fromEpochMilliseconds(date.timestamp())).totalSeconds * 1000
        return date.timestamp() - localTimeZoneOffset
    }

    actual fun startOfDay(): KDateTime? {
        return KDateTime(LocalDateTime(
            year = this.date.year,
            month = this.date.month,
            dayOfMonth = this.date.dayOfMonth,
            hour = 0,
            minute = 0
        ))
    }

    actual fun endOfDay(): KDateTime? {
        return KDateTime(LocalDateTime(
            year = this.date.year,
            month = this.date.month,
            dayOfMonth = this.date.dayOfMonth,
            hour = 23,
            minute = 59,
            second = 59,
            nanosecond = 999999999
        ))
    }

    actual fun plusDays(daysAmount: Int): KDateTime? {
        val timeZone = TimeZone.currentSystemDefault()
        val instant = this.date.toInstant(timeZone)
        val newInstant = instant.plus(daysAmount, DateTimeUnit.DAY, timeZone)
        return KDateTime(newInstant.toLocalDateTime(timeZone))
    }

    actual fun minusDays(daysAmount: Int): KDateTime? {
        val timeZone = TimeZone.currentSystemDefault()
        val instant = this.date.toInstant(timeZone)
        val newInstant = instant.minus(daysAmount, DateTimeUnit.DAY, timeZone)
        return KDateTime(newInstant.toLocalDateTime(timeZone))
    }

    actual fun after(date: KDateTime): Boolean {
        return this.timestamp() > date.timestamp()
    }

    actual fun between(
        start: KDateTime,
        end: KDateTime
    ): Boolean {
        return this.timestamp() in start.timestamp()..end.timestamp()
    }

    actual fun currentMonth(): Pair<KDateTime, KDateTime>? {
        val daysCount = this.date.month.getDaysAmount(this.date.year)
        val firstDayOfMonth = this.minusDays((this.date.dayOfMonth-1))?.startOfDay() ?: return null
        val lastDayOfMonth = this.plusDays(daysCount - this.date.dayOfMonth)?.endOfDay() ?: return null

        return Pair(firstDayOfMonth, lastDayOfMonth)
    }

    actual fun currentWeek(): Pair<KDateTime, KDateTime>? {
        val firstDayOfWeek = this.minusDays(this.date.dayOfWeek.isoDayNumber-1)?.startOfDay() ?: return null
        val lastDayOfWeek = this.plusDays(7-this.date.dayOfWeek.isoDayNumber)?.endOfDay() ?: return null

        return Pair(firstDayOfWeek, lastDayOfWeek)
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    actual fun formatToString(format: String): String {
        val dateFormatter = LocalDateTime.Format { byUnicodePattern(format) }
        return dateFormatter.format(this.date)
    }

    actual companion object;

    override fun equals(other: Any?): Boolean {
        return (other as? KDateTime)?.let {
            this.timestamp() == it.timestamp()
        } ?: false
    }

    actual override fun toString(): String {
        return formatToString("dd.MM.yyyy HH:mm")
    }
}