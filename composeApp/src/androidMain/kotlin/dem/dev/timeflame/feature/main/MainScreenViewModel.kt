package dem.dev.timeflame.feature.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dem.dev.timeflame.domain.manager.LocalAuthManager
import dem.dev.timeflame.data.preferences.KmpPreference
import dem.dev.timeflame.domain.model.Task
import dem.dev.timeflame.domain.repository.UserRepository
import dem.dev.timeflame.feature.calendar.model.Month
import dem.dev.timeflame.feature.calendar.usecase.LoadCalendarForMonthUseCase
import dem.dev.timeflame.feature.main.state.MainScreenState
import dem.dev.timeflame.feature.main.state.RecordingState
import dem.dev.timeflame.feature.main.state.VoiceInputState
import dem.dev.timeflame.feature.main.state.toggle
import dem.dev.timeflame.feature.task.usecase.CreateTaskUseCase
import dem.dev.timeflame.feature.task.usecase.DeleteTaskUseCase
import dem.dev.timeflame.feature.task.usecase.UpdateTaskUseCase
import dem.dev.timeflame.util.datetime.KDateTime
import dem.dev.timeflame.util.datetime.convertToZone
import dem.dev.timeflame.util.datetime.now
import dem.dev.timeflame.util.datetime.toLocalDateTime
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.state.UiMessageCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainScreenViewModel(
    private val localAuthManager: LocalAuthManager,
    private val userRepository: UserRepository,
    private val loadCalendarForMonthUseCase: LoadCalendarForMonthUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val calendarPreferences: KmpPreference
): ViewModel() {
    // Show the local month immediately even before the first network response.
    private val _state = MutableStateFlow(MainScreenState(currentMonth = Month.current()))
    val state = _state.asStateFlow()
    private var calendarRequestId = 0

    private fun selectionKey(): String = "genesis.calendar.day.${localAuthManager.getCurrentUser()?.id ?: "guest"}"

    private fun rememberDay(date: LocalDate) {
        calendarPreferences.put(selectionKey(), date.toString())
    }

    private fun openMonth(month: Month, requestedDay: Int) {
        if (month.days.isEmpty()) return
        val index = (requestedDay - 1).coerceIn(0, month.days.lastIndex)
        val selected = month.days[index]
        _state.update { it.copy(currentMonth = month, selectedDayIndex = index, selectedDay = selected) }
        rememberDay(LocalDate.of(month.year, month.number, selected.day.dayOfMonth))
        loadTasks(month)
    }

    fun onEvent(event: MainScreenEvent) {
        when(event) {
            is MainScreenEvent.CalendarDayClicked -> onCalendarDayClicked(event.calendarDayIndex)
            MainScreenEvent.CalendarViewSwitch -> onCalendarViewSwitched()
            is MainScreenEvent.DeleteTaskClicked -> onDeleteTaskClicked(event.task)
            is MainScreenEvent.EditTaskClicked -> onEditTaskClicked(event.task)
            MainScreenEvent.NextMonthBtnClicked -> onNextMonthClicked()
            MainScreenEvent.PreviousMonthBtnClicked -> onPreviousMonthClicked()
            is MainScreenEvent.TaskStatusChangeClicked -> onTaskStatusChangeClicked(event.task)
            MainScreenEvent.MessageDialogDismissed -> onMessageDialogDismissed()
            is MainScreenEvent.SaveUpdatedTaskBtnClicked -> onSaveUpdatedTaskBtnClicked(event.updatedTask)
            MainScreenEvent.EditTaskBottomSheetDismissed -> onEditTaskBottomSheetDismissed()
            MainScreenEvent.RecordNewTaskBtnClicked -> onRecordNewTaskBtnClicked()
            is MainScreenEvent.NewTaskRecordingFinished -> onNewTaskRecordingFinished(event.recognizedRequest)
            MainScreenEvent.NewTaskRecordingDismissed -> onNewTaskRecordingDismissed()
            is MainScreenEvent.RecognizedTaskTextEdited -> onRecognizedTaskTextEdited(event.updatedText)
            MainScreenEvent.CreateNewTaskClicked -> onCreateNewTaskClicked()
            MainScreenEvent.ProfileContextMenuClicked -> onProfileContextMenuClicked()
            MainScreenEvent.LogoutBtnClicked -> onLogoutBtnClicked()
            MainScreenEvent.ProfileContextMenuDismissed -> onProfileContextMenuDismissed()
        }
    }

    private fun onLogoutBtnClicked() {
        localAuthManager.signOut()
        _state.update { it.copy(logoutRequested = true) }
    }
    private fun onProfileContextMenuDismissed() {
        _state.update { it.copy(profileContextMenuOpened = false) }
    }
    private fun onProfileContextMenuClicked() {
        _state.update { it.copy(profileContextMenuOpened = !_state.value.profileContextMenuOpened) }
    }
    private fun onCreateNewTaskClicked() {
        val recordingText = _state.value.voiceInputState.currentRecognizedText

        if (_state.value.voiceInputState.recordingState == RecordingState.RecordingFinished && recordingText.isNotEmpty()) {
            createTask(recordingText)
        } else
            _state.update { it.copy(voiceInputState = VoiceInputState()) }
    }
    private fun onRecognizedTaskTextEdited(updatedText: String) {
        _state.update { it.copy(voiceInputState = it.voiceInputState.copy(currentRecognizedText = updatedText)) }
    }
    private fun onNewTaskRecordingDismissed() {
        _state.update { it.copy(voiceInputState = VoiceInputState(RecordingState.Idle)) }
    }
    private fun onNewTaskRecordingFinished(recognizedRequest: String) {
        _state.update { it.copy(voiceInputState = VoiceInputState(RecordingState.RecordingFinished, recognizedRequest)) }
    }
    private fun onRecordNewTaskBtnClicked() {
        _state.update { it.copy(voiceInputState = VoiceInputState(RecordingState.Recording, "")) }
    }
    private fun onMessageDialogDismissed() {
        _state.update { it.copy(screenState = ScreenState.Idle) }
    }
    private fun onCalendarDayClicked(calendarDayIndex: Int) {
        val month = _state.value.currentMonth ?: return
        val selected = month.days.getOrNull(calendarDayIndex) ?: return
        _state.update { it.copy(selectedDayIndex = calendarDayIndex, selectedDay = selected) }
        rememberDay(LocalDate.of(month.year, month.number, selected.day.dayOfMonth))
    }

    
    private fun onCalendarViewSwitched() {
        _state.update { it.copy(calendarViewState = it.calendarViewState.toggle()) }
    }
    private fun onDeleteTaskClicked(task: Task) {
        deleteTask(task)
    }
    private fun onEditTaskClicked(task: Task) {
        _state.update { it.copy(selectedTaskToEdit = task) }
    }
    private fun onSaveUpdatedTaskBtnClicked(updatedTask: Task) {
        if (_state.value.selectedTaskToEdit == null || _state.value.isSavingTask) return
        _state.update { it.copy(isSavingTask = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = updateTaskUseCase(updatedTask)
            if (result.isSuccess()) {
                _state.update { it.copy(isSavingTask = false, selectedTaskToEdit = null) }
                _state.value.currentMonth?.let { loadTasks(it) }
            } else {
                _state.update {
                    it.copy(
                        isSavingTask = false,
                        screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotAnErrorWhileCreatingTask)
                    )
                }
            }
        }
    }

    
    private fun onNextMonthClicked() {
        val previousSelection = _state.value.selectedDay.day.dayOfMonth
        _state.value.currentMonth?.next()?.let { openMonth(it, previousSelection) }
            ?: _state.update {
                it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.errorGettingNextMonth))
            }
    }

    private fun onPreviousMonthClicked() {
        val previousSelection = _state.value.selectedDay.day.dayOfMonth
        _state.value.currentMonth?.previous()?.let { openMonth(it, previousSelection) }
            ?: _state.update {
                it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.errorGettingPreviousMonth))
            }
    }

    
    private fun onTaskStatusChangeClicked(task: Task) {
        val updatedTask = task.copy(completed = !task.completed)
        val currentMonthDaysUpdated = _state.value.copy().currentMonth?.days?.map {
            val index = it.tasks.indexOf(task)
            if (index == -1) return@map it
            val newTasks = it.tasks.map { currTask ->
                if (currTask.id == updatedTask.id) updatedTask
                else currTask
            }.toMutableList()

            return@map it.copy(tasks = newTasks)
        } ?: listOf()

        _state.update {
            it.copy(currentMonth = it.currentMonth?.copy(days = currentMonthDaysUpdated.toMutableList()))
        }

        updateTaskStatus(updatedTask)
    }
    private fun onEditTaskBottomSheetDismissed() {
        _state.update { it.copy(selectedTaskToEdit = null) }
    }

    private fun loadTasks(month: Month) {
        val requestId = ++calendarRequestId
        _state.update { it.copy(isCalendarLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val user = localAuthManager.getCurrentUser()
            if (user == null) {
                if (requestId == calendarRequestId) {
                    _state.update {
                        it.copy(isCalendarLoading = false, screenState = ScreenState.Result(
                            ResultType.FAILURE, UiMessageCodes.gotErrorWhenGettingLocalUserId
                        ))
                    }
                }
                return@launch
            }
            val result = loadCalendarForMonthUseCase(user.id, month)
            if (requestId != calendarRequestId) return@launch

            if (result.isSuccess()) {
                // Rebuild fresh days: loading into the old mutable Month would duplicate tasks.
                val refreshed = Month.byNumberAndYear(month.number, month.year) ?: month
                refreshed.sortTasksByDays(result.data.orEmpty())
                _state.update { old ->
                    if (old.currentMonth?.year != month.year || old.currentMonth?.number != month.number) {
                        old.copy(isCalendarLoading = false)
                    } else {
                        val chosen = old.selectedDay.day.dayOfMonth.coerceIn(1, refreshed.days.size)
                        val index = chosen - 1
                        old.copy(
                            currentMonth = refreshed,
                            selectedDayIndex = index,
                            selectedDay = refreshed.days[index],
                            isCalendarLoading = false
                        )
                    }
                }
            } else {
                _state.update {
                    it.copy(isCalendarLoading = false, screenState = ScreenState.Result(
                        ResultType.FAILURE, UiMessageCodes.gotErrorLoadingTasks
                    ))
                }
            }
        }
    }

    
    private fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.Main) {
        val selectedDayIndex = _state.value.selectedDayIndex
        val selectedDayTasksCopy = mutableListOf(*(_state.value.currentMonth?.days ?: emptyList())[selectedDayIndex].tasks.toTypedArray())
        selectedDayTasksCopy.remove(task)

        val currentMonthDaysUpdated = _state.value.currentMonth?.days?.map {
            if (it == _state.value.currentMonth?.days?.get(selectedDayIndex)) it.copy(tasks = selectedDayTasksCopy) else it
        } ?: listOf()
        _state.update {
            it.copy(currentMonth = it.currentMonth?.copy(days = currentMonthDaysUpdated.toMutableList()))
        }

        withContext(Dispatchers.IO) {
            deleteTaskUseCase(task.id)
        }
    }


    private fun updateTaskStatus(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        updateTaskUseCase(task)
    }

    private fun restoreCalendar() {
        val today = LocalDate.now(ZoneId.systemDefault())
        val stored = runCatching {
            calendarPreferences.getString(selectionKey())?.let { LocalDate.parse(it) }
        }.getOrNull() ?: today
        val month = Month.byNumberAndYear(stored.monthValue, stored.year) ?: Month.current() ?: return
        openMonth(month, stored.dayOfMonth)
    }

    
    private fun createTask(taskRequest: String) {
        if (_state.value.isCreatingTask) return
        val selectedDate = _state.value.currentMonth?.days
            ?.getOrNull(_state.value.selectedDayIndex)?.day ?: KDateTime.now()
        // Apply the device's actual local clock to the selected calendar date.
        val now = LocalTime.now(ZoneId.systemDefault())
        val dateTime = LocalDate.of(
            selectedDate.year, selectedDate.monthNumber, selectedDate.dayOfMonth
        ).atTime(now.hour, now.minute).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT))

        val currentUser = localAuthManager.getCurrentUser()
        if (currentUser == null) {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotErrorWhenGettingLocalUserId)) }
            return
        }
        _state.update { it.copy(isCreatingTask = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = createTaskUseCase(userId = currentUser.id, taskText = taskRequest, dateTime = dateTime)
            if (result.isSuccess()) {
                _state.update {
                    it.copy(isCreatingTask = false, screenState = ScreenState.Result(
                        ResultType.SUCCESS, UiMessageCodes.taskCreatedSuccessfully
                    ), voiceInputState = VoiceInputState())
                }
                // Refresh exactly the month the user is viewing; never jump to today.
                _state.value.currentMonth?.let { loadTasks(it) }
            } else {
                _state.update {
                    it.copy(isCreatingTask = false, screenState = ScreenState.Result(
                        ResultType.FAILURE, UiMessageCodes.gotAnErrorWhileCreatingTask
                    ))
                }
            }
        }
    }

    
    private fun loadCurrentUser() = localAuthManager.getCurrentUser()?.let {
        viewModelScope.launch(Dispatchers.IO) {
            val result = userRepository.getUserById(it.id)
            if (result.isSuccess())
                _state.update { it.copy(currentUser = result.data) }
        }
    }

    private fun loadCurrentMonthTasks() {
        _state.value.currentMonth?.let { loadTasks(it) }
    }

    
    private fun updateDeviceToken() {
        localAuthManager.getCurrentUser()?.let { currentUser ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (!it.isSuccessful) {
                    Log.d("MainScreenViewModel", "Fetching FCM registration token failed", it.exception)
                    return@addOnCompleteListener
                }

                val token = it.result
                // saving the token on backend side
                viewModelScope.launch(Dispatchers.IO) {
                    userRepository.saveUserDeviceToken(currentUser.id, token)
                }
            }
        }
    }

    init {
        restoreCalendar()
        loadCurrentUser()
        // API v2 has no FCM device-token endpoint yet.
    }
}