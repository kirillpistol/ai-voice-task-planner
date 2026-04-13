package dem.dev.timeflame.feature.main.state

import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.model.User
import dem.dev.timeflame.feature.calendar.model.CalendarDay
import dem.dev.timeflame.feature.calendar.model.Month
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.now
import dem.dev.timeflame.util.state.ScreenState

data class MainScreenState(
    val screenState: ScreenState = ScreenState.Idle,
    val currentMonth: Month? = null,
    val currentUser: User? = null,
    val selectedDayIndex: Int = 0,
    val voiceInputState: VoiceInputState = VoiceInputState(),
    val selectedDay: CalendarDay = CalendarDay(KDateTime.now(), mutableListOf()),
    val calendarViewState: CalendarViewState = CalendarViewState.MONTH,
    val selectedTaskToEdit: Task? = null,
    val profileContextMenuOpened: Boolean = false,
    val logoutRequested: Boolean = false
)
