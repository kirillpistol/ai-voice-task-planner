package dem.dev.timeflame.util.datetime

expect class TimeZone

expect fun TimeZone.getTimeZoneOffset(): Int

expect val UtcTimeZone: TimeZone

expect val localTimeZone: TimeZone