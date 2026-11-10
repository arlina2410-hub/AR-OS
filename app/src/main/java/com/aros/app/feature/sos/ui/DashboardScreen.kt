package com.aros.app.feature.sos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.routine.domain.RoutineItem
import com.aros.app.feature.sos.domain.OverallReadinessStatus
import com.aros.app.feature.sos.domain.ReadinessReport
import com.aros.app.feature.sos.domain.SosConfig
import com.aros.app.feature.sos.domain.SosMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    readinessReport: ReadinessReport?,
    sosConfig: SosConfig,
    routines: List<RoutineItem>,
    onSimulateSosTrigger: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenFocus: () -> Unit,
    onAddRoutine: () -> Unit,
    onExploreFeatures: () -> Unit,
    onToggleRoutine: (RoutineItem) -> Unit
) {
    val unpassedChecks = readinessReport?.checks?.filter { !it.isPassing } ?: emptyList()
    val isSosReady = unpassedChecks.isEmpty() && readinessReport?.overallStatus != OverallReadinessStatus.NEEDS_ATTENTION
    val isLive = sosConfig.mode == SosMode.LIVE

    ArOsScreenScaffold(
        title = "AR/OS",
        subtitle = "Quiet, offline operating companion"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ArOsTheme.spacing.screenMargin)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. Finish Setup Banner (if any items missing or not completed)
            if (!isSosReady || !sosConfig.setupCompleted) {
                item {
                    val count = unpassedChecks.size.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ArOsTheme.colors.warning.copy(alpha = 0.15f))
                            .border(1.dp, ArOsTheme.colors.warning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { onOpenWizard() }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ArOsTheme.colors.warning, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Finish setup ($count remaining)",
                                        style = ArOsTheme.typography.body,
                                        fontWeight = FontWeight.Bold,
                                        color = ArOsTheme.colors.contentPrimary
                                    )
                                    Text(
                                        text = "Tap to review missing permissions and hardware setup.",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentSecondary
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.warning, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 2. Large Status Card: "Is SOS working?"
            item {
                ArOsCard {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            if (isSosReady) (if (isLive) ArOsTheme.colors.danger else ArOsTheme.colors.success) else ArOsTheme.colors.warning,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSosReady) {
                                        if (isLive) "SOS IS ON — LIVE MODE" else "SOS IS ON — TEST MODE"
                                    } else {
                                        "NEEDS SETUP (${unpassedChecks.size})"
                                    },
                                    style = ArOsTheme.typography.headline,
                                    color = if (isSosReady) (if (isLive) ArOsTheme.colors.danger else ArOsTheme.colors.success) else ArOsTheme.colors.warning
                                )
                            }

                            if (!isSosReady) {
                                ArOsButton(
                                    text = "Fix",
                                    onClick = onOpenPermissions,
                                    variant = ArOsButtonVariant.Primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isSosReady) {
                                if (isLive) "Press Volume Down 5 times to call your primary contact."
                                else "Safe test simulation active. Five presses will simulate without calling."
                            } else {
                                "One or more permissions or hardware filters need action before SOS can trigger."
                            },
                            style = ArOsTheme.typography.body,
                            color = ArOsTheme.colors.contentSecondary
                        )
                    }
                }
            }

            // 3. Quick Actions: "What can I do now?"
            item {
                ArOsGroup(header = "Quick Actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArOsButton(
                            text = "Test SOS",
                            onClick = onSimulateSosTrigger,
                            variant = ArOsButtonVariant.Secondary,
                            leadingIcon = Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f)
                        )
                        ArOsButton(
                            text = "Start Focus",
                            onClick = onOpenFocus,
                            variant = ArOsButtonVariant.Secondary,
                            leadingIcon = Icons.Default.CenterFocusStrong,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArOsButton(
                            text = "Add Routine",
                            onClick = onAddRoutine,
                            variant = ArOsButtonVariant.Ghost,
                            leadingIcon = Icons.Default.Add,
                            modifier = Modifier.weight(1f)
                        )
                        ArOsButton(
                            text = "What AR/OS Can Do",
                            onClick = onExploreFeatures,
                            variant = ArOsButtonVariant.Ghost,
                            leadingIcon = Icons.Default.Explore,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. "What needs my attention?" Next Due Routines
            item {
                ArOsGroup(header = "Daily Rhythms Due Next") {
                    if (routines.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No daily rhythms added yet.",
                                style = ArOsTheme.typography.body,
                                color = ArOsTheme.colors.contentSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ArOsButton(
                                text = "Create First Rhythm",
                                onClick = onAddRoutine,
                                variant = ArOsButtonVariant.Primary
                            )
                        }
                    } else {
                        routines.take(3).forEach { routine ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleRoutine(routine) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = routine.title,
                                        style = ArOsTheme.typography.body,
                                        color = if (routine.completedToday) ArOsTheme.colors.contentTertiary else ArOsTheme.colors.contentPrimary
                                    )
                                    Text(
                                        text = "${routine.formattedTime} • ${routine.durationMinutes} min",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentSecondary
                                    )
                                }
                                if (routine.completedToday) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = ArOsTheme.colors.success)
                                } else {
                                    Icon(Icons.Default.Schedule, contentDescription = "Pending", tint = ArOsTheme.colors.contentTertiary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
