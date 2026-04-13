package dem.dev.timeflame.feature.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dem.dev.timeflame.R
import dem.dev.timeflame.domain.model.User
import dem.dev.timeflame.feature.main.MainScreen
import dem.dev.timeflame.util.language.UiLanguageHelper
import dem.dev.timeflame.util.theme.AppTheme
import dem.dev.timeflame.util.theme.DarkModeHelper

@Composable
fun ProfilePopup(
    modifier: Modifier = Modifier,
    user: User,
    darkModeEnabled: Boolean,
    onLogoutClicked: () -> Unit,
    onDarkModeSwitchClicked: (Boolean) -> Unit
) {
    var isSettingsOpen by remember { mutableStateOf(false) }

    AppTheme(darkTheme = darkModeEnabled) {
        Card (
            modifier = modifier
                .padding(8.dp)
                .width(IntrinsicSize.Max),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(
                modifier = modifier
                    .padding(10.dp)
                    .width(IntrinsicSize.Min)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
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
                    )
                    Column(
                        modifier = Modifier.padding(start = 10.dp)
                    ) {
                        Text(user.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiary)
                        Text(user.email, modifier = Modifier.padding(top = 3.dp), color = MaterialTheme.colorScheme.onTertiary)
                    }
                }
                Divider(modifier = Modifier
                    .padding(top = 5.dp),
                    color = MaterialTheme.colorScheme.onTertiary
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
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 10.dp),
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Open button",
                        modifier = Modifier
                            .rotate(if (isSettingsOpen) 90f else -90f)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiary
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
                        tint = Color.Red
                    )
                    Text(
                        text = stringResource(R.string.logout),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 10.dp),
                        color = Color.Red
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
        UiLanguageHelper.setLanguage(context, languageCode)
        currentLanguageCode = languageCode
    }

    Column(
        modifier = modifier
            .padding(start = 15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dark_mode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiary
            )
            Switch(
                checked = isDarkThemeOn,
                onCheckedChange = { checked -> handleDarkModeSwitch(checked) },
                modifier = Modifier.padding(start = 10.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 5.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiary
            )

            Text(
                text = "\uD83C\uDDF7\uD83C\uDDFA",
                modifier = Modifier
                    .padding(start = 10.dp)
                    .border(width = 1.dp, color = if (currentLanguageCode == "ru") MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(10.dp))
                    .padding(5.dp)
                    .clickable { handleLanguageChange("ru") }
            )

            Text(
                text = "\uD83C\uDDEC\uD83C\uDDE7",
                modifier = Modifier
                    .padding(start = 5.dp)
                    .border(width = 1.dp, color = if (currentLanguageCode == "en") MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(10.dp))
                    .padding(5.dp)
                    .clickable { handleLanguageChange("en") }
            )
        }
    }
}