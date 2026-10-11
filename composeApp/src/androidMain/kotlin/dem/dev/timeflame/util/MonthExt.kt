package dem.dev.timeflame.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dem.dev.timeflame.R
import dem.dev.timeflame.feature.calendar.model.Month
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.now

@Composable
fun Month.getName(): String {
    return when(this.start.monthNumber) {
        1 -> stringResource(R.string.january)
        2 -> stringResource(R.string.february)
        3 -> stringResource(R.string.march)
        4 -> stringResource(R.string.april)
        5 -> stringResource(R.string.may)
        6 -> stringResource(R.string.june)
        7 -> stringResource(R.string.july)
        8 -> stringResource(R.string.august)
        9 -> stringResource(R.string.september)
        10 -> stringResource(R.string.october)
        11 -> stringResource(R.string.november)
        12 -> stringResource(R.string.december)
        else -> ""
    } + ", ${this.year}"
}