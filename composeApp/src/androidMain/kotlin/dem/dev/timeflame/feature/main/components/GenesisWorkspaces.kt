package dem.dev.timeflame.feature.main.components

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.graphics.Color
import dem.dev.timeflame.domain.model.Task
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import dem.dev.timeflame.feature.main.state.GenesisModuleOutput
import dem.dev.timeflame.feature.main.state.GenesisWorkspaceNotice
import dem.dev.timeflame.util.components.GenesisButtonProgress
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R
import dem.dev.timeflame.feature.main.MainScreenEvent
import dem.dev.timeflame.feature.main.state.MainScreenState
import dem.dev.timeflame.util.components.GenesisTaskSkeleton

/** Calendar task controls and separate SQL copies for personal analysis. */
@Composable
fun GenesisTasksWorkspace(
    state: MainScreenState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = state.genesisWorkspace
    val calendarTasks = state.currentMonth?.days.orEmpty().flatMap { it.tasks }.distinctBy { it.id }.sortedBy { it.timestamp }
    val context = LocalContext.current
    val policy = model.policy
    val consentAccepted = !policy?.policyVersion.isNullOrBlank() &&
        model.acceptedPolicyVersion == policy?.policyVersion
    var consent by remember(policy?.policyVersion) { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmCalendarDelete by remember { mutableStateOf<Task?>(null) }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WorkspaceTitle(
            stringResource(R.string.workspace_tasks_title),
            stringResource(R.string.workspace_tasks_subtitle)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                text = stringResource(R.string.genesis_sql_count, model.tasks.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.genesis_recommendations_count, model.recommendations.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.workspace_tasks_total, calendarTasks.size))
            Text(stringResource(R.string.workspace_tasks_completed, calendarTasks.count { it.completed }))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (model.loading) {
            GenesisTaskSkeleton()
        }
        if (model.failed) {
            Text(
                text = stringResource(R.string.genesis_sql_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (model.notice != GenesisWorkspaceNotice.NONE) {
            val message = when (model.notice) {
                GenesisWorkspaceNotice.CONNECTION_CHECKED -> when {
                    model.policy == null -> R.string.workspace_connection_failed
                    model.policy.importAvailable != true -> R.string.workspace_connection_not_ready
                    model.failed -> R.string.workspace_connection_failed
                    else -> R.string.workspace_connection_ready
                }
                GenesisWorkspaceNotice.IMPORTED -> R.string.genesis_sql_imported
                GenesisWorkspaceNotice.CLEARED -> R.string.genesis_sql_cleared
                GenesisWorkspaceNotice.TASK_DELETED -> R.string.genesis_sql_deleted
                GenesisWorkspaceNotice.NONE -> R.string.genesis_sql_imported
            }
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (calendarTasks.isEmpty() && model.tasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.workspace_tasks_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(calendarTasks, key = { "calendar:" + it.id }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = { onEvent(MainScreenEvent.TaskStatusChangeClicked(task)) },
                                enabled = !state.isSavingTask,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                            )
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(task.text, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    java.time.Instant.ofEpochMilli(task.timestamp)
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { onEvent(MainScreenEvent.EditTaskClicked(task)) },
                                    enabled = !state.isSavingTask
                                ) { Text(stringResource(R.string.edit_task)) }
                            }
                            IconButton(
                                onClick = { confirmCalendarDelete = task },
                                enabled = !state.isSavingTask
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.workspace_delete_task))
                            }
                        }
                    }
                }
                if (model.tasks.isNotEmpty()) {
                    item(key = "sql-heading") {
                        Text(stringResource(R.string.genesis_sql_tasks_title), style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(model.tasks, key = { "sql:" + it.sourceTaskId }) { task ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatGenesisDate(task.dueAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { confirmDeleteId = task.sourceTaskId },
                                enabled = !model.busy
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.genesis_sql_delete_one),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!consentAccepted) {
            if (policy?.policyUrl.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.genesis_sql_policy_missing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = consent,
                    onCheckedChange = { consent = it },
                    enabled = !model.busy && !model.loading
                )
                Text(
                    text = stringResource(R.string.genesis_sql_consent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).clickable(
                        enabled = !model.busy && !model.loading
                    ) { consent = !consent }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (consentAccepted) onEvent(MainScreenEvent.GenesisImportTasks)
                    else confirmImport = true
                },
                enabled = (consentAccepted || consent) &&
                    policy?.importAvailable == true && !model.busy && !model.loading,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (model.busy) GenesisButtonProgress()
                else Text(stringResource(R.string.genesis_sql_import_short))
            }
            OutlinedButton(
                onClick = { confirmClear = true },
                enabled = model.tasks.isNotEmpty() && !model.busy && !model.loading,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.genesis_sql_clear_short)) }
            IconButton(
                onClick = { onEvent(MainScreenEvent.GenesisWorkspaceRefresh) },
                enabled = !model.busy && !model.loading
            ) {
                Icon(Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.workspace_check_connection))
            }
        }
        if (!policy?.policyUrl.isNullOrBlank()) {
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policy!!.policyUrl)))
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.genesis_sql_policy_short),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    confirmCalendarDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { confirmCalendarDelete = null },
            title = { Text(stringResource(R.string.workspace_delete_task)) },
            text = { Text(stringResource(R.string.workspace_delete_task_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCalendarDelete = null
                    onEvent(MainScreenEvent.DeleteTaskClicked(task))
                }) { Text(stringResource(R.string.workspace_delete_task)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCalendarDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.genesis_sql_import)) },
            text = { Text(stringResource(R.string.genesis_sql_import_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    onEvent(MainScreenEvent.GenesisImportTasks)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.genesis_sql_clear)) },
            text = { Text(stringResource(R.string.genesis_sql_clear_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onEvent(MainScreenEvent.GenesisClearTasks)
                }) { Text(stringResource(R.string.genesis_sql_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    confirmDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(stringResource(R.string.genesis_sql_delete_one)) },
            text = { Text(stringResource(R.string.genesis_sql_delete_one_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteId = null
                    onEvent(MainScreenEvent.GenesisDeleteTask(id))
                }) { Text(stringResource(R.string.genesis_sql_delete_one)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun formatGenesisDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return runCatching {
        val instant = OffsetDateTime.parse(raw).toInstant()
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault()).format(instant)
    }.getOrElse { raw.take(16).replace("T", " ") }
}

@Composable
fun GenesisServicesWorkspace(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WorkspaceTitle(
            stringResource(R.string.workspace_services_title),
            stringResource(R.string.workspace_services_subtitle)
        )
        WorkspaceModuleCard(
            Icons.Default.GridView,
            stringResource(R.string.workspace_services_crypto),
            stringResource(R.string.workspace_services_crypto_desc),
            stringResource(R.string.workspace_coming_soon)
        )
        GenesisNftCarousel()
        WorkspaceModuleCard(
            Icons.Default.Description,
            stringResource(R.string.workspace_services_documents),
            stringResource(R.string.workspace_services_documents_desc),
            stringResource(R.string.workspace_coming_soon)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun GenesisReportsWorkspace(
    state: MainScreenState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val account = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
    val preferences = remember(context) {
        context.getSharedPreferences("genesis_sectors", Context.MODE_PRIVATE)
    }
    var selectedSector by rememberSaveable(account) {
        mutableStateOf(preferences.getString(account, null))
    }
    val sector = GenesisSector.values().firstOrNull { it.name == selectedSector }
    val language = if (LocalConfiguration.current.locales[0].language == "ru") "ru" else "en"
    LaunchedEffect(account, selectedSector, language) {
        sector?.let { onEvent(MainScreenEvent.GenesisMemoryRefresh(it.name, language)) }
    }
    // Sector output is independent of SQL recommendations and other modules.
    val output = sector?.let { state.genesisWorkspace.moduleOutputs[it.name] }
    var sourceChoice by rememberSaveable(account) { mutableStateOf("browser") }
    var searchEngine by rememberSaveable(account) { mutableStateOf("Google") }
    var sourceSite by rememberSaveable(account) { mutableStateOf("") }
    var resourceChoice by rememberSaveable(account) { mutableStateOf("service") }
    var resourceName by rememberSaveable(account) { mutableStateOf("") }
    var apkLabel by rememberSaveable(account) { mutableStateOf("") }
    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            apkLabel = uri.lastPathSegment?.takeLast(90) ?: "Выбран APK"
            resourceChoice = "apk"
        }
    }
    val sourceDescription = when (sourceChoice) {
        "website" -> "Сайт: " + (sourceSite.ifBlank { "не задан" })
        else -> "Поиск: $searchEngine"
    }
    val resourceDescription = when (resourceChoice) {
        "apk" -> "APK: " + apkLabel.ifBlank { "не выбран" }
        else -> "Сервис: " + resourceName.ifBlank { "не выбран" }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WorkspaceTitle(
            stringResource(R.string.workspace_genesis_title),
            stringResource(R.string.workspace_genesis_subtitle)
        )
        GenesisSectorSelector(selectedSector) { name ->
            selectedSector = name
            preferences.edit().putString(account, name).apply()
        }
        GenesisOutputWindow(
            output = output,
            selected = sector != null,
            sectorTitle = sector?.let { stringResource(it.titleRes) }
        )
        sector?.let {
            GenesisCommentComposer(state, it.name, language, onEvent)
        }
        GenesisSourceAndResourceControls(
            sourceChoice = sourceChoice,
            onSourceChoice = { sourceChoice = it },
            searchEngine = searchEngine,
            onSearchEngine = { searchEngine = it },
            sourceSite = sourceSite,
            onSourceSite = { sourceSite = it },
            resourceChoice = resourceChoice,
            onResourceChoice = { resourceChoice = it },
            resourceName = resourceName,
            onResourceName = { resourceName = it },
            apkLabel = apkLabel,
            onPickApk = { apkPicker.launch("application/vnd.android.package-archive") }
        )
        GenesisOutputDetail(
            stringResource(R.string.genesis_output_source),
            sourceDescription + "\n" + (output?.source ?: "Поиск ещё не выполнен"),
            stringResource(R.string.genesis_output_source_empty)
        )
        GenesisOutputDetail(
            stringResource(R.string.genesis_output_connector),
            resourceDescription + "\n" + (output?.connectionResource ?: "Адаптер ещё не подключён"),
            stringResource(R.string.genesis_output_connector_empty)
        )
        GenesisOutputDetail(
            stringResource(R.string.genesis_output_security),
            if (sector == GenesisSector.FINANCE) {
                "VPN - VM - DOC - VPN\n" +
                    if (language == "ru") "Профиль выбран · контур не развёрнут"
                    else "Profile selected · environment not deployed"
            } else sector?.securityProfileRes?.let { stringResource(it) },
            stringResource(R.string.genesis_output_security_empty)
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Source selection is configuration; it does not claim that a browser or APK was executed. */
@Composable
private fun GenesisSourceAndResourceControls(
    sourceChoice: String,
    onSourceChoice: (String) -> Unit,
    searchEngine: String,
    onSearchEngine: (String) -> Unit,
    sourceSite: String,
    onSourceSite: (String) -> Unit,
    resourceChoice: String,
    onResourceChoice: (String) -> Unit,
    resourceName: String,
    onResourceName: (String) -> Unit,
    apkLabel: String,
    onPickApk: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Источник проверки", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onSourceChoice("browser") },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (sourceChoice == "browser") MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surface)) { Text("Поиск") }
            OutlinedButton(onClick = { onSourceChoice("website") },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (sourceChoice == "website") MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surface)) { Text("Сайт") }
        }
        if (sourceChoice == "browser") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Google", "Bing", "DuckDuckGo").forEach { engine ->
                    OutlinedButton(onClick = { onSearchEngine(engine) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (searchEngine == engine) MaterialTheme.colorScheme.primaryContainer
                                             else MaterialTheme.colorScheme.surface)) {
                        Text(engine, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = sourceSite, onValueChange = onSourceSite,
                label = { Text("Адрес сайта (https://...)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }
        HorizontalDivider()
        Text("Ресурс", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onResourceChoice("service") }) { Text("Сервис") }
            OutlinedButton(onClick = { onResourceChoice("apk") }) { Text("APK") }
        }
        if (resourceChoice == "service") {
            OutlinedTextField(
                value = resourceName, onValueChange = onResourceName,
                label = { Text("Имя подключаемого сервиса") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        } else {
            OutlinedButton(onClick = onPickApk) { Text("Выбрать APK на телефоне") }
            Text(
                apkLabel.ifBlank { "APK не выбран" },
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text("Выбор сохранён на экране. Проверка источника и обучение адаптера требуют отдельного запуска Level 3.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GenesisCommentComposer(
    state: MainScreenState,
    sector: String,
    language: String,
    onEvent: (MainScreenEvent) -> Unit
) {
    val model = state.genesisWorkspace
    val context = LocalContext.current
    val policy = model.memoryPolicy
    val busy = model.memoryBusySector != null
    val pending = model.pendingRules[sector]
    val rules = model.memoryRules[sector].orEmpty()
    val kind = model.commentKinds[sector] ?: "constraint"
    val draft = model.commentDrafts[sector].orEmpty()
    var consent by remember(sector, policy?.policyVersion) { mutableStateOf(false) }
    var showRules by rememberSaveable(sector) { mutableStateOf(false) }
    var forgetId by remember(sector) { mutableStateOf<String?>(null) }
    val kinds = listOf(
        "goal" to R.string.genesis_memory_goal,
        "constraint" to R.string.genesis_memory_constraint,
        "preference" to R.string.genesis_memory_preference,
        "correction" to R.string.genesis_memory_correction
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.genesis_memory_comment),
                modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            IconButton(
                onClick = { onEvent(MainScreenEvent.GenesisMemoryRefresh(sector, language)) },
                enabled = !busy
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.workspace_check_connection))
            }
        }
        kinds.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (key, label) ->
                    OutlinedButton(
                        onClick = { onEvent(MainScreenEvent.GenesisCommentKindSelected(sector, key)) },
                        modifier = Modifier.weight(1f).semantics { selected = kind == key },
                        enabled = model.memoryBusySector != sector,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (kind == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (kind == key) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text(stringResource(label), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { onEvent(MainScreenEvent.GenesisCommentEdited(sector, it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = model.memoryBusySector != sector,
            minLines = 3, maxLines = 6,
            label = { Text(stringResource(R.string.genesis_memory_comment)) },
            supportingText = { Text(stringResource(R.string.genesis_memory_limit, draft.length)) }
        )
        if (model.memoryFailedSector == sector) {
            Text(stringResource(R.string.genesis_memory_failed),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else if (policy?.available != true && !busy) {
            Text(stringResource(R.string.genesis_memory_unavailable),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (pending == null) {
            Button(
                onClick = { onEvent(MainScreenEvent.GenesisCommentPreview(sector, language)) },
                enabled = policy?.available == true && draft.isNotBlank() && !busy
            ) {
                if (model.memoryBusySector == sector) GenesisButtonProgress()
                else Text(stringResource(R.string.genesis_memory_preview))
            }
        } else {
            if (!policy?.policyUrl.isNullOrBlank()) {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policy!!.policyUrl)))
                }) { Text(stringResource(R.string.genesis_memory_terms)) }
            }
            if (!model.memoryConsentAccepted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !busy)
                    Text(stringResource(R.string.genesis_memory_consent),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).clickable(enabled = !busy) { consent = !consent })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onEvent(MainScreenEvent.GenesisCommentConfirmed(sector, consent)) },
                    enabled = !busy && policy?.available == true && (consent || model.memoryConsentAccepted)
                ) {
                    if (model.memoryBusySector == sector) GenesisButtonProgress()
                    else Text(stringResource(R.string.genesis_memory_confirm))
                }
                TextButton(
                    onClick = { onEvent(MainScreenEvent.GenesisCommentDiscarded(sector, language)) },
                    enabled = !busy
                ) { Text(stringResource(R.string.genesis_memory_discard)) }
            }
        }
        TextButton(onClick = { showRules = !showRules }) {
            Text(stringResource(R.string.genesis_memory_rules, rules.size))
        }
        if (showRules) {
            rules.forEach { rule ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.text, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { forgetId = rule.id }, enabled = !busy) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.genesis_memory_forget))
                    }
                }
            }
        }
    }
    forgetId?.let { id ->
        AlertDialog(
            onDismissRequest = { forgetId = null },
            title = { Text(stringResource(R.string.genesis_memory_forget)) },
            text = { Text(stringResource(R.string.genesis_memory_forget_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    forgetId = null
                    onEvent(MainScreenEvent.GenesisRuleForgotten(sector, id, language))
                }) { Text(stringResource(R.string.genesis_memory_forget)) }
            },
            dismissButton = { TextButton(onClick = { forgetId = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun GenesisOutputWindow(
    output: GenesisModuleOutput?,
    selected: Boolean,
    sectorTitle: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                sectorTitle ?: stringResource(R.string.genesis_output_title),
                style = MaterialTheme.typography.titleMedium
            )
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                contentAlignment = if (output?.text.isNullOrBlank())
                    Alignment.Center else Alignment.TopStart
            ) {
                SelectionContainer {
                    Text(
                        text = output?.text?.takeIf { it.isNotBlank() }
                            ?: stringResource(if (selected)
                                R.string.genesis_output_waiting else R.string.genesis_output_choose),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (output?.text.isNullOrBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun GenesisOutputDetail(title: String, value: String?, emptyText: String) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            SelectionContainer {
                Text(
                    value?.takeIf { it.isNotBlank() } ?: emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class GenesisSector(val titleRes: Int, val securityProfileRes: Int? = null) {
    FINANCE(R.string.genesis_sector_finance, R.string.genesis_security_finance),
    MEDICINE(R.string.genesis_sector_medicine),
    MANUFACTURING(R.string.genesis_sector_manufacturing),
    MILITARY(R.string.genesis_sector_military),
    GOVERNMENT(R.string.genesis_sector_government),
    LOGISTICS(R.string.genesis_sector_logistics)
}

/** Sector selection only; processing is connected separately on the server. */
@Composable
private fun GenesisSectorSelector(
    selectedSector: String?,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.genesis_sector_prompt),
            style = MaterialTheme.typography.titleSmall
        )
        GenesisSector.values().toList().chunked(2).forEach { sectors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sectors.forEach { sector ->
                    val isSelected = selectedSector == sector.name
                    OutlinedButton(
                        onClick = { onSelected(sector.name) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .semantics { selected = isSelected },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            stringResource(sector.titleRes),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp, end = 48.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** These entries intentionally have no onClick until a real module/API exists. */
@Composable
private fun WorkspaceModuleCard(
    icon: ImageVector,
    title: String,
    detail: String,
    status: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                status?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

