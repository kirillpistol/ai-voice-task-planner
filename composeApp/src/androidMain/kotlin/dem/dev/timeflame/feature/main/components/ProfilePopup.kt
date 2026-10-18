package dem.dev.timeflame.feature.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dem.dev.timeflame.R
import dem.dev.timeflame.domain.model.User
import dem.dev.timeflame.data.repository.GenesisAiUsage
import dem.dev.timeflame.feature.main.MainScreen
import dem.dev.timeflame.util.language.UiLanguageHelper
import dem.dev.timeflame.util.theme.AppTheme
import dem.dev.timeflame.util.theme.DarkModeHelper

@Composable
fun ProfilePopup(
    modifier: Modifier = Modifier,
    user: User,
    aiUsage: GenesisAiUsage?,
    darkModeEnabled: Boolean,
    onLogoutClicked: () -> Unit,
    onDarkModeSwitchClicked: (Boolean) -> Unit
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🪙 Запросы ИИ", color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        aiUsage?.let { "${it.used}/${it.limit}" } ?: "—/—",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
