package dem.dev.timeflame.util.datetime

expect class KDateTime {
    val monthNumber: Int
    val year: Int
    val dayOfMonth: Int
    val dayOfWeek: Int

    /**
     * Get timestamp in millis in local timezone
     */
    fun timestamp(): Long

    /**
     * Get timestamp in millis in UTC (converted from local to UTC timezone using offset)
     */
    fun utcTimestamp(): Long

    /**
     * Get start of the day in local timezone
     */
    fun startOfDay(): KDateTime?

    /**
     * Get end of the day in local timezone
     */
    fun endOfDay(): KDateTime?

    /**
     * Add days to the current date in local timezone
     */
    fun plusDays(daysAmount: Int): KDateTime?

    /**
     * Minus days from the current date in local timezone
     */
    fun minusDays(daysAmount: Int): KDateTime?

    /**
     * Returns true if the current date is after date: KDateTime in local timezone
     */
    fun after(date: KDateTime): Boolean

    /**
     * Check if the current date is placed between 2 dates: start and end in local timezone
     */
    fun between(start: KDateTime, end: KDateTime): Boolean

    /**
     * Get current month borders starting from the 1st day of month 00:00 until the last day 23:59:59:999
     */
    fun currentMonth(): Pair<KDateTime, KDateTime>?

    /**
     * Get current week borders starting from Monday 00:00 until Sunday 23:59:59:999
     */
    fun currentWeek(): Pair<KDateTime, KDateTime>?

    /**
     * Format current date to string
     */
    fun formatToString(format: String): String

    override fun toString(): String

    companion object;
}

/**
 * Get current KDateTime in the local timezone
 */
expect fun KDateTime.Companion.now(): KDateTime

/**
 * Get KDateTime from local timezone
 */
expect fun KDateTime.Companion.fromTimestamp(timestamp: Long): KDateTime

/**
 * Get KDateTime from UTC timezone
 */
expect fun KDateTime.Companion.fromUtcTimestamp(timestamp: Long): KDateTime

/**
 * Get KDateTime by date in local timezone
 */
expect fun KDateTime.Companion.fromDate(
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0
): KDateTime?