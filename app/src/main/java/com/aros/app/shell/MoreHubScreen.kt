package com.aros.app.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme

data class MoreItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val destination: AppDestination
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
    onNavigateTo: (AppDestination) -> Unit
) {
    val items = listOf(
        MoreItem("Activity Ledger", "Screen interactions timeline and daily intentional focus summary.", Icons.Default.QueryStats, AppDestination.LEDGER),
        MoreItem("Digital Guardian", "On-device inspection of camera, mic, and location permissions.", Icons.Default.Shield, AppDestination.GUARDIAN),
        MoreItem("Focus Rules", "Mindful friction gates before launching distracting apps.", Icons.Default.Lock, AppDestination.DISCIPLINE),
        MoreItem("Insights", "On-device habit analysis summarizing your sovereignty trends.", Icons.Default.Insights, AppDestination.ADAPTIVE),
        MoreItem("Permissions & Health", "Check system permissions, SIM selection, and accessibility status.", Icons.Default.Security, AppDestination.PERMISSIONS),
        MoreItem("Safety Settings", "Configure countdown duration, volume restore, and speakerphone.", Icons.Default.Settings, AppDestination.SOS_SETTINGS),
        MoreItem("Trigger Lab", "Live hardware Volume Down press testing and interval telemetry.", Icons.Default.Speed, AppDestination.SOS_LAB),
        MoreItem("SOS History", "Audit log of all emergency triggers, countdowns, and tests.", Icons.Default.History, AppDestination.SOS_LOG),
        MoreItem("What SOS Can't Do", "Safety boundaries, offline convenience disclaimer, and guidelines.", Icons.Default.HelpOutline, AppDestination.SOS_HELP),
        MoreItem("Design Gallery", "Explore M3 dark glassmorphic components and typography.", Icons.Default.Palette, AppDestination.GALLERY)
    )

    ArOsScreenScaffold(
        title = "More Features",
        subtitle = "Settings, auxiliary tools, and system health"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ArOsTheme.spacing.screenMargin)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)
        ) {
            items(items.size) { index ->
                val item = items[index]
                ArOsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateTo(item.destination) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = ArOsTheme.colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = ArOsTheme.typography.body,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ArOsTheme.colors.contentTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
