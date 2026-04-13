package dem.dev.timeflame

import dem.dev.timeflame.feature.calendar.model.Month
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.UtcTimeZone
import dem.dev.timeflame.util.datetime.fromDate
import dem.dev.timeflame.util.datetime.fromTimestamp
import dem.dev.timeflame.util.datetime.now
import platform.Foundation.NSDate
import kotlin.test.Test
import kotlin.test.assertEquals

class KDateTimeTest {
    @Test
    fun testCurrentDayOfWeek() {
        val currentDate = KDateTime(2024, 12, 15)
        assertEquals(7, currentDate.dayOfWeek)
    }

    @Test
    fun testCurrentMonth() {
        val currentMonth = Month.current()

        val startDate = KDateTime.fromDate(2024, 12, 1)?.startOfDay()?: return
        val endDate = KDateTime.fromDate(2024, 12, 31)?.endOfDay()?: return
        val month = Month(startDate, endDate, emptyList())

        assertEquals(month.start, currentMonth?.start)
        assertEquals(month.end, currentMonth?.end)
    }

    @Test
    fun testNextMonth() {
        val currentMonth = Month.current()
        val nextMonth = Month(KDateTime(2025, 1, 1, 0, 0, 0), KDateTime(2025, 1, 31, 23, 59, 59), emptyList())
        val nextMonthFromCurrent = currentMonth?.next()

        assertEquals(nextMonth.start, nextMonthFromCurrent?.start)
        assertEquals(nextMonth.end, nextMonthFromCurrent?.end)
    }

    @Test
    fun textNextDay() {
        val currentDay = KDateTime.now(UtcTimeZone).startOfDay()

        val nextDay = KDateTime(2024, 12, 20)
        assertEquals(nextDay, currentDay?.plusDays(1))
    }

    @Test
    fun testStartOfDay() {
        val expected = KDateTime(2024, 12, 20, 0, 0, 0)
        val day = KDateTime(2024, 12, 19, 23, 0, 0)

        assertEquals(expected, day.plusDays(1)?.startOfDay())
    }

    @Test
    fun testKDateTimeFromTimestamp() {
        val timestamp = 1734786107000
        val date = KDateTime.fromTimestamp(timestamp)
        assertEquals(KDateTime.now(UtcTimeZone).startOfDay(), date.startOfDay())
    }
}