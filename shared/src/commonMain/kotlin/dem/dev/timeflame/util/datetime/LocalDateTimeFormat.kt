package dem.dev.timeflame.util.datetime

import dem.dev.timeflame.domain.model.AppLanguage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.char

object Format {
    fun dateTimeFormat1(appLanguage: AppLanguage) = LocalDateTime.Format {
        dayOfMonth(); char('.'); monthNumber(); char('.'); year(); if (appLanguage == AppLanguage.RUS) chars(" в ") else chars(" at "); hour(); char(':'); minute()
    }
    @OptIn(FormatStringsInDatetimeFormats::class)
    val dateFormat = LocalDateTime.Format {
        byUnicodePattern("dd.MM.yyyy")
    }
    @OptIn(FormatStringsInDatetimeFormats::class)
    val timeFormat = LocalDateTime.Format {
        byUnicodePattern("HH:mm")
    }
}

fun String.toLocalDateTime(format: DateTimeFormat<LocalDateTime>): LocalDateTime {
    return format.parse(this)
}

fun LocalDateTime.formatToString(format: DateTimeFormat<LocalDateTime>): String {
    return format.format(this)
}