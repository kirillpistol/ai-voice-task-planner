package dem.dev.timeflame.feature.main.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R
import dem.dev.timeflame.feature.main.MainScreenEvent
import dem.dev.timeflame.feature.main.state.MainScreenState
import dem.dev.timeflame.util.components.GenesisTaskSkeleton

/** Read-only SQL task copies. Calendar is the only editor and source of tasks. */
@Composable
fun GenesisTasksWorkspace(
    state: MainScreenState,
    onEvent: (MainScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = state.genesisWorkspace
    val context = LocalContext.current
    var consent by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        WorkspaceTitle(
            stringResource(R.string.genesis_sql_tasks_title),
            stringResource(R.string.genesis_sql_tasks_subtitle)
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
        if (model.tasks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.genesis_sql_empty),
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
                items(model.tasks, key = { it.sourceTaskId }) { task ->
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

        val policy = model.policy
        if (!policy?.policyUrl.isNullOrBlank()) {
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policy!!.policyUrl)))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.genesis_sql_policy_link))
            }
        } else {
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
                enabled = policy?.importAvailable == true && !model.busy
            )
            Text(
                text = stringResource(R.string.genesis_sql_consent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(enabled = policy?.importAvailable == true && !model.busy) {
                    consent = !consent
                }
            )
        }
        Button(
            onClick = { confirmImport = true },
            enabled = consent && policy?.importAvailable == true && !model.busy && !model.loading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (model.busy) GenesisButtonProgress()
            else Text(stringResource(R.string.genesis_sql_import))
        }
        if (model.tasks.isNotEmpty()) {
            OutlinedButton(
                onClick = { confirmClear = true },
                enabled = !model.busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.genesis_sql_clear))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.genesis_sql_import)) },
            text = { Text(stringResource(R.string.genesis_sql_import_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    consent = false
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
        WorkspaceModuleCard(
            Icons.Default.Star,
            stringResource(R.string.workspace_services_nft),
            stringResource(R.string.workspace_services_nft_desc),
            stringResource(R.string.workspace_coming_soon)
        )
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
    modifier: Modifier = Modifier
) {
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
        if (state.genesisWorkspace.recommendations.isNotEmpty()) {
            state.genesisWorkspace.recommendations.take(20).forEach { recommendation ->
                WorkspaceModuleCard(
                    Icons.Default.AutoAwesome,
                    stringResource(R.string.genesis_recommendation_title),
                    recommendation.summary
                )
            }
        }
        WorkspaceModuleCard(
            Icons.Default.Description,
            stringResource(R.string.workspace_genesis_reports),
            stringResource(R.string.workspace_genesis_reports_desc),
            stringResource(R.string.workspace_coming_soon)
        )
        WorkspaceModuleCard(
            Icons.Default.GridView,
            stringResource(R.string.workspace_genesis_sources),
            stringResource(R.string.workspace_genesis_sources_desc),
            stringResource(R.string.workspace_coming_soon)
        )
        WorkspaceModuleCard(
            Icons.Default.AutoAwesome,
            stringResource(R.string.workspace_genesis_recommendations),
            stringResource(R.string.workspace_genesis_recommendations_desc),
            stringResource(R.string.workspace_coming_soon)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WorkspaceTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp),
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
