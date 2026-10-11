package dem.dev.timeflame.feature.main.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dem.dev.timeflame.R
import dem.dev.timeflame.feature.main.state.MainSection

/**
 * Permanent four-tab navigation within the authenticated Main screen.
 * Does not create new activities or reset the calendar view model.
 */
@Composable
fun GenesisBottomBar(
    selected: MainSection,
    onSelected: (MainSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = listOf(
        Triple(MainSection.CALENDAR, R.string.tab_calendar, Icons.Default.DateRange),
        Triple(MainSection.TASKS, R.string.tab_tasks, Icons.Default.List),
        Triple(MainSection.SERVICES, R.string.tab_services, Icons.Default.GridView),
        Triple(MainSection.GENESIS, R.string.tab_genesis, Icons.Default.GridView)
    )
    NavigationBar(
        modifier = modifier.fillMaxWidth().height(76.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        sections.forEach { (section, labelRes, image) ->
            val title = stringResource(labelRes)
            NavigationBarItem(
                selected = selected == section,
                onClick = { onSelected(section) },
                icon = {
                    if (section == MainSection.GENESIS) {
                        // Angular G from the GENESIS brand lockup, not a generic text glyph.
                        Icon(
                            painter = painterResource(R.drawable.ic_genesis_mark),
                            modifier = Modifier.size(24.dp),
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            imageVector = image,
                            contentDescription = null
                        )
                    }
                },
                label = { Text(text = title, maxLines = 1) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
