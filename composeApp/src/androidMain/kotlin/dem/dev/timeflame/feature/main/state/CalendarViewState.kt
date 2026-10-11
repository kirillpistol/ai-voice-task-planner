package dem.dev.timeflame.feature.main.state

enum class CalendarViewState {
    WEEK, MONTH
}

fun CalendarViewState.toggle(): CalendarViewState {
    return when(this) {
        CalendarViewState.MONTH -> CalendarViewState.WEEK
        CalendarViewState.WEEK -> CalendarViewState.MONTH
    }
}