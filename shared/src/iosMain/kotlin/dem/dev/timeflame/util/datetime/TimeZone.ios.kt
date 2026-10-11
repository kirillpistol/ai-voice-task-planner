package dem.dev.timeflame.util.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toKotlinTimeZone
import platform.Foundation.NSTimeZone
import platform.Foundation.create
import platform.Foundation.localTimeZone

actual typealias TimeZone = NSTimeZone

actual val UtcTimeZone: TimeZone
    get() = NSTimeZone.Companion.create("UTC") ?: NSTimeZone.Companion.localTimeZone

actual val localTimeZone: TimeZone
    get() = NSTimeZone.Companion.localTimeZone

actual fun TimeZone.getTimeZoneOffset(): Int {
    return this.toKotlinTimeZone().offsetAt(Clock.System.now()).totalSeconds / 3600
}