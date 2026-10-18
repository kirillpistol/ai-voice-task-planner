package dem.dev.timeflame.feature.main

import dem.dev.timeflame.feature.main.state.MainSection
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.feature.calendar.model.CalendarDay

sealed interface MainScreenEvent {
    data class MainSectionSelected(val section: MainSection): MainScreenEvent
    data object GenesisWorkspaceRefresh: MainScreenEvent
    data object GenesisCoreStatusRefresh: MainScreenEvent
    data object GenesisImportTasks: MainScreenEvent
    data object GenesisClearTasks: MainScreenEvent
    data class GenesisDeleteTask(val sourceTaskId: String): MainScreenEvent
    data class GenesisAiAnalyze(val sector: String): MainScreenEvent
    data class GenesisCommentEdited(val sector: String, val text: String): MainScreenEvent
    data class GenesisCommentKindSelected(val sector: String, val kind: String): MainScreenEvent
    data class GenesisMemoryRefresh(val sector: String, val language: String): MainScreenEvent
    data class GenesisCommentPreview(val sector: String, val language: String): MainScreenEvent
    data class GenesisCommentConfirmed(val sector: String, val consent: Boolean): MainScreenEvent
    data class GenesisCommentDiscarded(val sector: String, val language: String): MainScreenEvent
    data class GenesisRuleForgotten(val sector: String, val ruleId: String, val language: String): MainScreenEvent
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
    data object NewTaskRecordingProcessing: MainScreenEvent
    data class NewTaskRecordingFailed(val messageCode: Int): MainScreenEvent
    data object NewTaskRecordingDismissed: MainScreenEvent
    data class RecognizedTaskTextEdited(val updatedText: String): MainScreenEvent
    data object CreateNewTaskClicked: MainScreenEvent
    data object ProfileContextMenuClicked: MainScreenEvent
    data object ProfileContextMenuDismissed: MainScreenEvent
    data object LogoutBtnClicked: MainScreenEvent
}
