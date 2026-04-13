package dem.dev.timeflame.feature.main

import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.feature.calendar.model.CalendarDay

sealed interface MainScreenEvent {
    data object NextMonthBtnClicked: MainScreenEvent
    data object PreviousMonthBtnClicked: MainScreenEvent
    data object CalendarViewSwitch: MainScreenEvent
    data class CalendarDayClicked(val calendarDayIndex: Int): MainScreenEvent
    data class EditTaskClicked(val task: Task): MainScreenEvent
    data class DeleteTaskClicked(val task: Task): MainScreenEvent
    data class TaskStatusChangeClicked(val task: Task): MainScreenEvent
    data object MessageDialogDismissed: MainScreenEvent
    data class SaveUpdatedTaskBtnClicked(val updatedTask: Task): MainScreenEvent
    data object EditTaskBottomSheetDismissed: MainScreenEvent
    data object RecordNewTaskBtnClicked: MainScreenEvent
    data class NewTaskRecordingFinished(val recognizedRequest: String): MainScreenEvent
    data object NewTaskRecordingDismissed: MainScreenEvent
    data class RecognizedTaskTextEdited(val updatedText: String): MainScreenEvent
    data object CreateNewTaskClicked: MainScreenEvent
    data object ProfileContextMenuClicked: MainScreenEvent
    data object ProfileContextMenuDismissed: MainScreenEvent
    data object LogoutBtnClicked: MainScreenEvent
}