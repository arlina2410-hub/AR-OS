package com.aros.app.feature.sos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsRow
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.TriggerTelemetry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerLabScreen(
    telemetry: TriggerTelemetry,
    onResetTelemetry: () -> Unit,
    onClearCooldown: () -> Unit
) {
    ArOsScreenScaffold(
        title = "Trigger Lab"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Column {
                        Text(
                            text = "TRIGGER DIAGNOSTIC LAB",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hardware Key Telemetry",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Press Volume Down repeatedly on your phone right now to verify timing, debouncing, and sliding-window recognition in real-time.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )
                    }
                }
            }

            // 1. Live Dots Visualizer Card
            item {
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("telemetry_dots_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RAPID PRESS PROGRESS",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentTertiary
                        )

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..5) {
                                val isActive = i <= telemetry.currentCount
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isActive) ArOsTheme.colors.sosCritical else ArOsTheme.colors.surfaceRaised
                                        )
                                        .border(
                                            1.dp,
                                            if (isActive) ArOsTheme.colors.sosCritical else ArOsTheme.colors.hairline,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$i",
                                        style = ArOsTheme.typography.numericTitle,
                                        color = if (isActive) Color.White else ArOsTheme.colors.contentTertiary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        Text(
                            text = if (telemetry.isCooldownActive) "Cooldown Active (5s safety lockout)"
                            else "${telemetry.currentCount} of 5 presses detected",
                            style = ArOsTheme.typography.headline,
                            color = if (telemetry.isCooldownActive) ArOsTheme.colors.warning else ArOsTheme.colors.contentPrimary
                        )
                    }
                }
            }

            // 2. Press Gaps & Intervals Inset Group
            item {
                ArOsGroup(
                    header = "Recent Inter-Press Intervals",
                    footer = "Target window: 65ms (debounce) to 850ms (timeout)"
                ) {
                    if (telemetry.intervals.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ArOsTheme.spacing.md)
                        ) {
                            Text(
                                text = "No intervals recorded yet. Tap Volume Down repeatedly.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentTertiary
                            )
                        }
                    } else {
                        telemetry.intervals.forEachIndexed { index, gap ->
                            val isValid = gap in 65..850
                            ArOsRow(
                                title = "Press ${index + 1} → Press ${index + 2}",
                                trailingContent = {
                                    Text(
                                        text = "${gap}ms",
                                        style = ArOsTheme.typography.numericBody,
                                        color = if (isValid) ArOsTheme.colors.success else ArOsTheme.colors.danger
                                    )
                                },
                                showDivider = index < telemetry.intervals.size - 1
                            )
                        }
                    }
                }
            }

            // 3. Diagnostics & Rejection Reason
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Column {
                        Text(
                            text = "Diagnostic Status",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = telemetry.lastRejectionReason ?: "None — sequence proceeding normally",
                            style = ArOsTheme.typography.body,
                            color = if (telemetry.lastRejectionReason != null) ArOsTheme.colors.warning else ArOsTheme.colors.success
                        )
                    }
                }
            }

            // 4. Lab Controls
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ArOsTheme.spacing.screenMargin),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArOsButton(
                        text = "Clear Count",
                        onClick = onResetTelemetry,
                        variant = ArOsButtonVariant.Secondary,
                        leadingIcon = Icons.Default.Refresh,
                        modifier = Modifier.weight(1f)
                    )

                    if (telemetry.isCooldownActive) {
                        ArOsButton(
                            text = "Bypass Cooldown",
                            onClick = onClearCooldown,
                            variant = ArOsButtonVariant.Primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
