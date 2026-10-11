package dem.dev.timeflame.util.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.offsetAt

actual typealias TimeZone = kotlinx.datetime.TimeZone

actual fun TimeZone.getTimeZoneOffset(): Int {
    return this.offsetAt(Clock.System.now()).totalSeconds / 3600 /* Converting offset in seconds to hours */
}

actual val UtcTimeZone: TimeZone = TimeZone.UTC

actual val localTimeZone: TimeZone = TimeZone.currentSystemDefault()