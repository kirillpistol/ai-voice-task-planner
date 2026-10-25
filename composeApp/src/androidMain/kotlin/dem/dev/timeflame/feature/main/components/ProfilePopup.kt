package dem.dev.timeflame.feature.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dem.dev.timeflame.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import dem.dev.timeflame.domain.model.User
import dem.dev.timeflame.feature.main.MainScreen
import dem.dev.timeflame.util.language.UiLanguageHelper
import dem.dev.timeflame.util.theme.AppTheme
import dem.dev.timeflame.util.theme.DarkModeHelper

/** Read-only, authenticated server counter; PISTOL monetary balance is separate. */
internal suspend fun genesisRequestUsage(): Pair<Int, Int>? {
    val user = FirebaseAuth.getInstance().currentUser ?: return null
    val token = suspendCancellableCoroutine<String?> { continuation ->
        user.getIdToken(false).addOnCompleteListener { result ->
            if (continuation.isActive) continuation.resume(
                if (result.isSuccessful) result.result?.token else null
            )
        }
    } ?: return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://api.pistolgenesis.ru/api/v2/genesis/ai/billing/summary")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("Authorization", "Bearer $token")
            try {
                if (connection.responseCode != 200) return@runCatching null
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val used = json.optInt("used", -1)
                val limit = json.optInt("limit", -1)
                if (used < 0 || limit <= 0) null else used to limit
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}

@Composable
private fun GenesisUsageHeader(onReportClick: (Pair<Int, Int>?) -> Unit) {
    val context = LocalContext.current
    var usage by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(FirebaseAuth.getInstance().currentUser?.uid) {
        loading = true
        usage = genesisRequestUsage()
        loading = false
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accent = if (dark) Color(0xFFD5B65E) else Color(0xFF806016)
    val name = "pistol_token_micro"
    val imageId = remember(context) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
            .takeIf { it != 0 }
            ?: context.resources.getIdentifier("pistol_token", "drawable", context.packageName)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (imageId != 0) {
                Image(
                    painter = painterResource(imageId),
                    contentDescription = "PISTOL",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(30.dp)
                )
            } else {
                Text("P", color = accent, style = MaterialTheme.typography.titleMedium)
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Запросы GENESIS", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Text("Израсходовано / всего", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (loading) "…" else usage?.let { "${it.first} / ${it.second}" } ?: "— / —",
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text("Эффективность: —", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Потенциал: —", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Открыть обновляемый отчёт в документах",
            tint = accent,
            modifier = Modifier.size(26.dp).clickable { onReportClick(usage) }
        )
    }
}

@Composable
fun ProfilePopup(
    modifier: Modifier = Modifier,
    user: User,
    darkModeEnabled: Boolean,
    onLogoutClicked: () -> Unit,
    onDarkModeSwitchClicked: (Boolean) -> Unit,
    onGenesisReportClick: (Pair<Int, Int>?) -> Unit
) {
    var isSettingsOpen by remember { mutableStateOf(false) }
    // Keep the menu inside the screen on compact phones, rather than sizing to the flags.
    val popupWidth = minOf(300, (LocalConfiguration.current.screenWidthDp - 28).coerceAtLeast(220)).dp

    AppTheme(darkTheme = darkModeEnabled) {
        Card (
            modifier = modifier
                .padding(8.dp)
                .width(popupWidth),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_genesis_mark),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    ) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = user.email,
                            modifier = Modifier.padding(top = 3.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                GenesisUsageHeader(onGenesisReportClick)
                Divider(modifier = Modifier
                    .padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(15.dp)
                        .fillMaxWidth()
                        .clickable { isSettingsOpen = !isSettingsOpen }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings icon",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Open button",
                        modifier = Modifier
                            .rotate(if (isSettingsOpen) 90f else -90f)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isSettingsOpen) {
                    SettingsSection(darkModeEnabled = darkModeEnabled, onDarkModeSwitchClicked = onDarkModeSwitchClicked)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(10.dp)
                        .clickable { onLogoutClicked() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Logout icon",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.logout),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    darkModeEnabled: Boolean,
    onDarkModeSwitchClicked: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isDarkThemeOn by remember { mutableStateOf( darkModeEnabled ) }
    var currentLanguageCode by remember { mutableStateOf(UiLanguageHelper.getCurrentLanguage(context)) }

    val handleDarkModeSwitch: (Boolean) -> Unit = { checked ->
        isDarkThemeOn = checked
        onDarkModeSwitchClicked(checked)
        DarkModeHelper.setDarkModeFlag(checked, context)
    }

    val handleLanguageChange: (String) -> Unit = { languageCode ->
        if (currentLanguageCode != languageCode) {
            currentLanguageCode = languageCode
            UiLanguageHelper.setLanguage(context, languageCode)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.dark_mode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = isDarkThemeOn,
                onCheckedChange = handleDarkModeSwitch,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ui_language),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageFlag(
                    symbol = "\uD83C\uDDF7\uD83C\uDDFA",
                    name = stringResource(R.string.language_russian),
                    isSelected = currentLanguageCode == "ru",
                    onClick = { handleLanguageChange("ru") }
                )
                LanguageFlag(
                    symbol = "\uD83C\uDDEC\uD83C\uDDE7",
                    name = stringResource(R.string.language_english),
                    isSelected = currentLanguageCode == "en",
                    onClick = { handleLanguageChange("en") }
                )
            }
        }
    }
}

/**
 * Two equally sized, accessible language targets. No ring, no border around emoji.
 * The active language has only a thin centred underline.
 */
@Composable
private fun LanguageFlag(
    symbol: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(48.dp)
            .height(48.dp)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = symbol,
            modifier = Modifier.height(29.dp),
            fontSize = 23.sp,
            lineHeight = 29.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(21.dp)
                .height(2.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )
    }
}
