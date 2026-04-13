package dem.dev.timeflame.feature.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dem.dev.timeflame.R
import dem.dev.timeflame.feature.calendar.model.Month
import dem.dev.timeflame.feature.main.components.EditTaskBottomSheet
import dem.dev.timeflame.feature.main.components.ProfilePopup
import dem.dev.timeflame.feature.main.components.TaskItem
import dem.dev.timeflame.feature.main.state.CalendarViewState
import dem.dev.timeflame.feature.main.state.MainScreenState
import dem.dev.timeflame.feature.main.state.RecordingState
import dem.dev.timeflame.navigation.Screen
import dem.dev.timeflame.util.androidKoinViewModel
import dem.dev.timeflame.util.components.Calendar
import dem.dev.timeflame.util.components.LoadingDialog
import dem.dev.timeflame.util.components.MessageDialog
import dem.dev.timeflame.util.components.MessageType
import dem.dev.timeflame.util.components.Screen
import dem.dev.timeflame.util.getMessage
import dem.dev.timeflame.util.getName
import dem.dev.timeflame.util.language.UiLanguageHelper
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.theme.AppTheme
import dem.dev.timeflame.util.theme.DarkModeHelper
import java.util.ArrayList


@Composable
fun MainScreen(
    navigateTo: (Screen) -> Unit,
    popBackStack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = androidKoinViewModel<MainScreenViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val newTaskRecordingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        (result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as? ArrayList<String>)?.let {
            viewModel.onEvent(MainScreenEvent.NewTaskRecordingFinished(it[0]))
        } ?: run {
            viewModel.onEvent(MainScreenEvent.NewTaskRecordingDismissed)
        }
    }

    val notificationsPermissionRequestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
        notificationsPermissionRequestLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

    LaunchedEffect(state.voiceInputState) {
        if (state.voiceInputState.recordingState == RecordingState.Recording) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                UiLanguageHelper.getGoogleVoiceRecognitionLanguage(context)
            )
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.dictate_your_task))
            newTaskRecordingLauncher.launch(intent)
        }
    }

    LaunchedEffect(state.logoutRequested) {
        if (state.logoutRequested) {
            popBackStack()
            navigateTo(Screen.Login)
        }
    }

    MainScreenView(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenView(
    modifier: Modifier = Modifier,
    state: MainScreenState = MainScreenState(),
    onEvent: (MainScreenEvent) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()

    val isDarkModeEnabled = DarkModeHelper.isDarkModeEnabled()
    val isSystemInDarkMode = isSystemInDarkTheme()
    var darkModeEnabled by remember { mutableStateOf(isDarkModeEnabled ?: isSystemInDarkMode) }

    (state.screenState as? ScreenState.Loading)?.let {
        LoadingDialog(
            isLoading = true,
            message = getMessage(it.messageCode)
        )
    }
    (state.screenState as? ScreenState.Result)?.let {
        val messageType = when (it.resultType) {
            ResultType.SUCCESS -> MessageType.SUCCESS
            ResultType.FAILURE -> MessageType.ERROR
        }
        MessageDialog(
            dialog = MessageDialog(
                type = messageType,
                message = getMessage(it.messageCode)
            ),
            onDismiss = { onEvent(MainScreenEvent.MessageDialogDismissed )}
        )
    }

    AppTheme(darkTheme = darkModeEnabled) {
        Screen(
            modifier = modifier
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clickable (
                        onClick = {
                            onEvent(MainScreenEvent.ProfileContextMenuDismissed)
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Header(
                        modifier = Modifier
                    )

                    CalendarSection(
                        modifier = Modifier
                            .padding(top = 25.dp),
                        state = state,
                        onEvent = onEvent
                    )

                    TasksList(
                        modifier = Modifier
                            .padding(top = 15.dp),
                        currentMonth = state.currentMonth,
                        selectedDayIndex = state.selectedDayIndex,
                        onEvent = onEvent
                    )

                    Spacer(modifier = Modifier.fillMaxHeight().weight(1f))

                    state.selectedTaskToEdit?.let {
                        ModalBottomSheet(
                            sheetState = sheetState,
                            onDismissRequest = { onEvent(MainScreenEvent.EditTaskBottomSheetDismissed)}
                        ) {
                            EditTaskBottomSheet(
                                task = it,
                                onDismissRequest = { onEvent(MainScreenEvent.EditTaskBottomSheetDismissed) },
                                onTaskSave = { onEvent(MainScreenEvent.SaveUpdatedTaskBtnClicked(it)) }
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Image(
                        painter = if (darkModeEnabled)
                            painterResource(R.drawable.timeflame_icon_dark)
                        else
                            painterResource(R.drawable.timeflame_icon_light),
                        contentDescription = null,
                        modifier = Modifier
                            .size(55.dp)
                            .padding(5.dp)
                            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable (
                                onClick = {
                                    onEvent(MainScreenEvent.ProfileContextMenuClicked)
                                },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                    )

                    if (state.profileContextMenuOpened && state.currentUser != null) {
                        ProfilePopup(
                            user = state.currentUser,
                            darkModeEnabled = darkModeEnabled,
                            onLogoutClicked = { onEvent(MainScreenEvent.LogoutBtnClicked) },
                            onDarkModeSwitchClicked = { darkModeEnabled = it }
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    NewTaskRecordSection(
                        modifier = Modifier.padding(bottom = 10.dp),
                        state = state,
                        onEvent = onEvent
                    )
                }

            }

        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                )) {
                    append(stringResource(R.string.your))
                }
                append("  ")
                withStyle(SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                )) {
                    append(stringResource(R.string.tasks))
                }
            }
        )
    }
}

@Composable
private fun CalendarSection(
    modifier: Modifier = Modifier,
    state: MainScreenState = MainScreenState(),
    onEvent: (MainScreenEvent) -> Unit = {}
) {
    // there are 2 types of calendar view: monthly and weekly
    val calendarDays = when(state.calendarViewState) {
        CalendarViewState.WEEK -> state.currentMonth?.getWeekForDay(state.selectedDay)?.days
        CalendarViewState.MONTH -> state.currentMonth?.days
    } ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxWidth(0.9f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    disabledContainerColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier
                    .clickable { onEvent(MainScreenEvent.PreviousMonthBtnClicked) }
            ) {
                Image(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    modifier = Modifier
                        .size(35.dp)
                        .padding(10.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiary)
                )
            }

            Text(
                text = state.currentMonth?.getName()?: "",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onTertiary
            )

            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    disabledContainerColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier
                    .clickable { onEvent(MainScreenEvent.NextMonthBtnClicked) }
            ) {
                Image(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    modifier = Modifier
                        .size(35.dp)
                        .padding(10.dp)
                        .rotate(180f),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiary)
                )
            }
        }
        
        Calendar(
            modifier = Modifier
                .padding(top = 15.dp),
            calendarDays = calendarDays,
            selectedDayIndex = state.selectedDayIndex,
            onDayClicked = { onEvent(MainScreenEvent.CalendarDayClicked(it)) }
        )
    }
}

@Composable
private fun CalendarViewSwitcher(
    modifier: Modifier = Modifier,
    state: MainScreenState = MainScreenState(),
    onEvent: (MainScreenEvent) -> Unit = {}
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(15.dp))
            .padding(horizontal = 15.dp, vertical = 3.dp)
            .clickable { onEvent(MainScreenEvent.CalendarViewSwitch) }
    ) {
        // if current calendar view state is month we need to rotate icon to the top and if state is weeek we need to rotate it down
        val iconRotation = when(state.calendarViewState) {
            CalendarViewState.MONTH -> 90f
            CalendarViewState.WEEK -> -90f
        }
        Image(
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .rotate(iconRotation),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiary)
        )
    }
}

@Composable
private fun TasksList(
    modifier: Modifier = Modifier,
    currentMonth: Month?,
    selectedDayIndex: Int,
    onEvent: (MainScreenEvent) -> Unit = {}
) {
    if (currentMonth?.days?.isNotEmpty() == true && currentMonth.days.size >= selectedDayIndex+1) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth(0.9f)
        ) {
            items(currentMonth.days[selectedDayIndex].tasks) { task ->
                TaskItem(
                    modifier = Modifier.padding(top = 10.dp),
                    task = task,
                    onEditClicked = { onEvent(MainScreenEvent.EditTaskClicked(task)) },
                    onDeleteClicked = { onEvent(MainScreenEvent.DeleteTaskClicked(task)) },
                    onStatusChangeClicked = { onEvent(MainScreenEvent.TaskStatusChangeClicked(task)) }
                )
            }
        }
    }
}

@Composable
private fun NewTaskRecordSection(
    modifier: Modifier = Modifier,
    state: MainScreenState = MainScreenState(),
    onEvent: (MainScreenEvent) -> Unit = {}
) {
    when(state.voiceInputState.recordingState) {
        RecordingState.Idle -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp, end = 40.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = { onEvent(MainScreenEvent.RecordNewTaskBtnClicked) },
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
        RecordingState.Recording -> {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    stringResource(R.string.recording),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(15.dp)
                )
            }
        }
        RecordingState.RecordingFinished -> {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = state.voiceInputState.currentRecognizedText,
                        onValueChange = { onEvent(MainScreenEvent.RecognizedTaskTextEdited(it)) },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(15.dp),
                        colors = androidx.compose.material.TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = MaterialTheme.colorScheme.onBackground,
                            textColor = MaterialTheme.colorScheme.onTertiary,
                            disabledTextColor = MaterialTheme.colorScheme.onTertiary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Button(
                        onClick = { onEvent(MainScreenEvent.CreateNewTaskClicked) },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colorScheme.onBackground,
                            disabledBackgroundColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(top = 20.dp),
                        contentPadding = PaddingValues(vertical = 17.dp)
                    ) {
                        Image(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onTertiary)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun CalendarViewSwitcherPreview() {
    AppTheme {
        CalendarViewSwitcher()
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    AppTheme {
        MainScreenView()
    }
}