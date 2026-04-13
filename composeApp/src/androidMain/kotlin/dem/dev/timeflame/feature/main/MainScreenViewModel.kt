package dem.dev.timeflame.feature.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dem.dev.timeflame.domain.manager.LocalAuthManager
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

class MainScreenViewModel(
    private val localAuthManager: LocalAuthManager,
    private val userRepository: UserRepository,
    private val loadCalendarForMonthUseCase: LoadCalendarForMonthUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
): ViewModel() {
    private val _state = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()

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
        _state.update { it.copy(selectedDayIndex = calendarDayIndex) }
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
        if (_state.value.selectedTaskToEdit == null)
            return

        val selectedTaskToEdit = _state.value.selectedTaskToEdit!!
        _state.update { it.copy(selectedTaskToEdit = null) }

        val currentMonthDaysUpdated = _state.value.copy().currentMonth?.days?.mapIndexed { i, it ->
            val updatedTaskDateTime = updatedTask.timestamp.toLocalDateTime().convertToZone(TimeZone.UTC, TimeZone.currentSystemDefault())
            if (it.day.monthNumber != updatedTaskDateTime.monthNumber || it.day.year != updatedTaskDateTime.year) {
                Month.byNumberAndYear(updatedTaskDateTime.monthNumber, updatedTaskDateTime.year)?.let { month ->
                    updateTaskAndGotoMonth(updatedTask, month, updatedTaskDateTime.dayOfMonth)
                }
                return
            }

            // if in updated task date was changed we need to add this task to another day
            if (it.day.dayOfMonth == updatedTaskDateTime.dayOfMonth && it.day.dayOfMonth != selectedTaskToEdit.timestamp.toLocalDateTime().convertToZone(
                    TimeZone.UTC, TimeZone.currentSystemDefault()).dayOfMonth) {
                _state.update { prevState -> prevState.copy(selectedDayIndex = i) }
                return@mapIndexed it.copy(tasks = (it.tasks + updatedTask).toMutableList())
            }

            // if there is no such task in the current day just return current day
            if (!it.tasks.contains(selectedTaskToEdit)) return@mapIndexed it.copy()

            // if we found task in the current day, but while editing task date was changed and it now is not in the current day
            if (it.day.dayOfMonth != updatedTaskDateTime.dayOfMonth) {
                return@mapIndexed it.copy(
                    tasks = (it.tasks - selectedTaskToEdit).toMutableList()
                )
            }
            val newTasks = (it.tasks - selectedTaskToEdit + updatedTask).toMutableList()

            return@mapIndexed it.copy(tasks = newTasks)
        } ?: listOf()

        updateTask(updatedTask)

        _state.update {
            it.copy(currentMonth = it.currentMonth?.copy(days = currentMonthDaysUpdated.toMutableList()), selectedTaskToEdit = null)
        }
    }
    private fun onNextMonthClicked() {
        _state.value.currentMonth?.next()?.let { nextMonth ->
            _state.update { it.copy(selectedDayIndex = 0) }
            // loading tasks for the next month (that is already current)
            loadTasks(nextMonth)
        } ?: run {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.errorGettingNextMonth)) }
        }
    }
    private fun onPreviousMonthClicked() {
        _state.value.currentMonth?.previous()?.let { previousMonth ->
            _state.update { it.copy(selectedDayIndex = 0) }

            // loading tasks for the previous month (that is already current)
            loadTasks(previousMonth)
        } ?: run {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.errorGettingPreviousMonth)) }
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

    private fun loadTasks(month: Month) = viewModelScope.launch(Dispatchers.IO) {
        _state.update { it.copy(screenState = ScreenState.Loading(UiMessageCodes.loadingTasks)) }

        localAuthManager.getCurrentUser()?.let { user ->
            val result = loadCalendarForMonthUseCase(user.id, month)

            if (result.isSuccess()) {
                result.data?.let { tasks ->
                    val newMonth = month.sortTasksByDays(tasks)
                    _state.update { it.copy(screenState = ScreenState.Idle, currentMonth = Month(newMonth.start, newMonth.end, newMonth.days)) }
                }
            } else {
                _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotErrorLoadingTasks)) }
            }
        } ?: run {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotErrorWhenGettingLocalUserId)) }
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

    private fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        updateTaskUseCase(task)
    }

    private fun updateTaskAndGotoMonth(task: Task, month: Month, newTaskDayOfMonth: Int) = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            _state.update { it.copy(screenState = ScreenState.Loading()) }
        }

        updateTaskUseCase(task)
        loadTasks(month)

        withContext(Dispatchers.Main) {
            _state.update { it.copy(
                selectedDayIndex = newTaskDayOfMonth - 1 // for example day with number 8 is with index 7 in the list
            ) }
        }
    }

    // function to get default day (today) when opening the calendar
    private fun getCurrentDay() {
        _state.update { it.copy(selectedDayIndex = KDateTime.now().dayOfMonth-1) }
    }

    private fun createTask(taskRequest: String) {
        localAuthManager.getCurrentUser()?.let { currUser ->
            _state.update { it.copy(screenState = ScreenState.Loading()) }

            viewModelScope.launch(Dispatchers.IO) {
                val dateTime = _state.value.currentMonth?.days?.let { it[_state.value.selectedDayIndex].day.formatToString("dd.MM.yyyy HH:mm") } ?: KDateTime.now().formatToString("dd.MM.yyyy HH:mm")
                val result = createTaskUseCase(userId = currUser.id, taskText = taskRequest, dateTime = dateTime)

                if (result.isSuccess()) {
                    _state.update { it.copy(screenState = ScreenState.Result(ResultType.SUCCESS, UiMessageCodes.taskCreatedSuccessfully), voiceInputState = VoiceInputState()) }
                    loadCurrentMonthTasks()
                }
                else
                    _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotAnErrorWhileCreatingTask), voiceInputState = VoiceInputState()) }
            }
        } ?: run {
            _state.update { it.copy(screenState = ScreenState.Result(ResultType.FAILURE, UiMessageCodes.gotErrorWhenGettingLocalUserId)) }
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
        Month.current()?.let { currentMonth ->
            loadTasks(currentMonth)
        }
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
        loadCurrentMonthTasks()
        getCurrentDay()
        loadCurrentUser()
        updateDeviceToken()
    }
}