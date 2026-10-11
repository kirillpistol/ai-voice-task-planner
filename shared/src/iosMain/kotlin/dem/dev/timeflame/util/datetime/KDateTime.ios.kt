package dem.dev.timeflame.util.datetime

import androidx.compose.runtime.internal.composableLambda
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toKotlinTimeZone
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarMatchStrictly
import platform.Foundation.NSCalendarOptions
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekOfYear
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitWeekdayOrdinal
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSCalendarUnitYearForWeekOfYear
import platform.Foundation.NSCalendarWrapComponents
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeIntervalSince1970
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.Foundation.now
import platform.Foundation.secondsFromGMT
import platform.Foundation.timeIntervalSince1970
import platform.posix.localtime
import platform.posix.time
import platform.posix.times

private fun createNSDate(
    year: Int = 0,
    month: Int = 0,
    day: Int = 0,
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0
): NSDate? {
    val components = NSDateComponents().apply {
        this.year = year.toLong()
        this.month = month.toLong()
        this.day = day.toLong()
        this.hour = hour.toLong()
        this.minute = minute.toLong()
        this.second = second.toLong()
    }
    return NSCalendar.currentCalendar.dateFromComponents(components)
}

actual fun KDateTime.Companion.now(): KDateTime {
    val date = NSDate.now()
    return KDateTime(date)
}

actual fun KDateTime.Companion.fromTimestamp(
    timestamp: Long
): KDateTime {
    val offset = 978307200000L //timestamp is from jan 01 1970, NSDate needs to get timestamp from jan 01 2001
    val timestampFromReferenceDate = (timestamp - offset)/1000 // reference date - 01.01.2001
    return KDateTime(date = NSDate(timestampFromReferenceDate.toDouble()))
}

actual fun KDateTime.Companion.fromUtcTimestamp(timestamp: Long): KDateTime {
    val offset = 978307200000L //timestamp is from jan 01 1970, NSDate needs to get timestamp from jan 01 2001
    val timeZoneOffset = localTimeZone.secondsFromGMT()
    val timestampFromReferenceDate = (timestamp - offset)/1000 + timeZoneOffset // reference date - 01.01.2001
    return KDateTime(date = NSDate(timestampFromReferenceDate.toDouble()))
}

actual fun KDateTime.Companion.fromDate(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int
): KDateTime? {
    val date = createNSDate(
        year = year,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
    ) ?: return null

    return KDateTime(date)
}

actual class KDateTime(private val date: NSDate = NSDate.now()) {

    actual val monthNumber: Int
        get() = getCurrentMonthNumber()

    actual val year: Int
        get() = getCurrentYear()

    actual val dayOfMonth: Int
        get() = getCurrentDay()

    actual val dayOfWeek: Int
        get() = getCurrentDayOfWeek()

    constructor(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): this(createNSDate(
        year = year,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
    )?:NSDate.now())

    constructor(year: Int, month: Int, day: Int): this(createNSDate(
        year = year,
        month = month,
        day = day,
        hour = 0,
        minute = 0,
        second = 0
    )?:NSDate.now())

    actual fun timestamp(): Long {
        return date.timeIntervalSince1970.toLong() * 1000;
    }

    actual fun utcTimestamp(): Long {
        return (date.timeIntervalSince1970.toLong() - localTimeZone.secondsFromGMT()) * 1000
    }

    actual fun startOfDay(): KDateTime? {
        val calendar = NSCalendar.currentCalendar
        val nsDate = createNSDate(
            year = calendar.component(NSCalendarUnitYear, this@KDateTime.date).toInt(),
            month = calendar.component(NSCalendarUnitMonth, this@KDateTime.date).toInt(),
            day = calendar.component(NSCalendarUnitDay, this@KDateTime.date).toInt(),
            hour = 0,
            minute = 0
        ) ?: return null
        return KDateTime(nsDate)
    }

    actual fun endOfDay(): KDateTime? {
        val calendar = NSCalendar.currentCalendar
        val nsDate = createNSDate(
            year = calendar.component(NSCalendarUnitYear, this@KDateTime.date).toInt(),
            month = calendar.component(NSCalendarUnitMonth, this@KDateTime.date).toInt(),
            day = calendar.component(NSCalendarUnitDay, this@KDateTime.date).toInt(),
            hour = 23,
            minute = 59,
            second = 59
        ) ?: return null
        return KDateTime(nsDate)
    }

    actual fun plusDays(daysAmount: Int): KDateTime? {
        val calendar = NSCalendar.currentCalendar
        calendar.timeZone = NSTimeZone.localTimeZone
        val components = NSDateComponents()
        components.day = 1

        return calendar.dateByAddingComponents(components, date, NSCalendarMatchStrictly)?.let { KDateTime(it) }
    }

    actual fun minusDays(daysAmount: Int): KDateTime? {
        val calendar = NSCalendar.currentCalendar
        calendar.timeZone = NSTimeZone.localTimeZone
        val components = NSDateComponents()
        components.day = -1

        return calendar.dateByAddingComponents(components, date, NSCalendarMatchStrictly)?.let { KDateTime(it) }
    }

    actual fun after(date: KDateTime): Boolean {
        return timestamp() > date.timestamp()
    }

    actual fun between(
        start: KDateTime,
        end: KDateTime
    ): Boolean {
        return timestamp() in start.timestamp()..end.timestamp()
    }

    actual companion object;

    override fun equals(other: Any?): Boolean {
        return (other as? KDateTime)?.let {
            this.timestamp() == it.timestamp()
        } ?: false
    }

    private fun getStartOfMonthForDate(date: NSDate): NSDate? {
        val calendar = NSCalendar.currentCalendar
        calendar.timeZone = NSTimeZone.localTimeZone

        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth,
            date
        )

        return calendar.dateFromComponents(components)
    }

    private fun getEndOfMonthForDate(date: NSDate): NSDate? {
        val calendar = NSCalendar.currentCalendar
        calendar.timeZone = NSTimeZone.localTimeZone
        val components = NSDateComponents()
        components.month = 1
        components.second = -1

        val startOfMonth = getStartOfMonthForDate(date)?: return null
        return calendar.dateByAddingComponents(components, startOfMonth, NSCalendarMatchStrictly)
    }

    actual fun currentMonth(): Pair<KDateTime, KDateTime>? {
        val startOfMonth = getStartOfMonthForDate(date)?: return null
        val firstDayKDateTime = KDateTime(startOfMonth)

        val endOfMonth = getEndOfMonthForDate(date)?: return null
        val lastDayKDateTime = KDateTime(endOfMonth)

        return Pair(firstDayKDateTime, lastDayKDateTime)
    }

    actual fun currentWeek(): Pair<KDateTime, KDateTime>? {
        val calendar = NSCalendar.currentCalendar

        val components = calendar.components(
            NSCalendarUnitYearForWeekOfYear or NSCalendarUnitWeekOfYear,
            this.date
        )
        val startOfWeekDate = calendar.dateFromComponents(components) ?: return null
        val startOfWeekKDateTime = KDateTime(startOfWeekDate).startOfDay() ?: return null

        val endOfWeekOffset = calendar.weekdaySymbols.count() - 2
        val endOfWeekComponents = NSDateComponents().apply {
            day = endOfWeekOffset.toLong()
            hour = 23L
            minute = 59L
            second = 59L
        }

        val endOfWeekDate = calendar.dateByAddingComponents(endOfWeekComponents, toDate = startOfWeekDate, options = NSCalendarMatchStrictly) ?: return null
        val endOfWeekKDateTime = KDateTime(endOfWeekDate).endOfDay() ?: return null

        return Pair(startOfWeekKDateTime, endOfWeekKDateTime)
    }

    actual fun formatToString(format: String): String {
        val formatter = NSDateFormatter()
        formatter.dateFormat = format
        return formatter.stringFromDate(this.date)
    }

    private fun getCurrentMonthNumber(): Int {
        val calendar = NSCalendar.currentCalendar
        return calendar.component(NSCalendarUnitMonth, this.date).toInt()
    }

    private fun getCurrentYear(): Int {
        val calendar = NSCalendar.currentCalendar
        return calendar.component(NSCalendarUnitYear, this.date).toInt()
    }

    private fun getCurrentDay(): Int {
        val calendar = NSCalendar.currentCalendar
        return calendar.component(NSCalendarUnitDay, this.date).toInt()
    }

    private fun getCurrentDayOfWeek(): Int {
        val calendar = NSCalendar.currentCalendar
        val dayOfWeek = calendar.component(NSCalendarUnitWeekday, this.date).toInt() - 1
        return if (dayOfWeek == 0) 7 else dayOfWeek
    }

    actual override fun toString(): String {
        return formatToString("dd.MM.yyyy HH:mm")
    }
}

