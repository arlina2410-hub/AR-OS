package com.aros.app.feature.focus.ui

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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.focus.domain.FocusMode
import com.aros.app.feature.focus.domain.FocusTimerState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    timerState: FocusTimerState,
    todayFocusMinutes: Int,
    onSelectMode: (FocusMode) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onLogInterruption: () -> Unit
) {
    val activeMode = when (timerState) {
        is FocusTimerState.Idle -> timerState.mode
        is FocusTimerState.Running -> timerState.mode
        is FocusTimerState.Paused -> timerState.mode
        is FocusTimerState.Completed -> timerState.mode
    }

    val secondsRemaining = when (timerState) {
        is FocusTimerState.Running -> timerState.secondsRemaining
        is FocusTimerState.Paused -> timerState.secondsRemaining
        is FocusTimerState.Completed -> 0
        is FocusTimerState.Idle -> activeMode.minutes * 60
    }

    val minutesPart = secondsRemaining / 60
    val secondsPart = secondsRemaining % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutesPart, secondsPart)

    ArOsScreenScaffold(
        title = "Deep Focus"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. Mode Selector Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ArOsTheme.spacing.screenMargin),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusMode.entries.forEach { mode ->
                        FilterChip(
                            selected = activeMode == mode,
                            onClick = { onSelectMode(mode) },
                            enabled = timerState is FocusTimerState.Idle,
                            label = { Text("${mode.label} (${mode.minutes}m)", style = ArOsTheme.typography.caption) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ArOsTheme.colors.accent,
                                selectedLabelColor = ArOsTheme.colors.contentOnAccent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. High-Precision Circular Timer Ring
            item {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(ArOsTheme.colors.surface)
                        .border(1.5.dp, ArOsTheme.colors.hairline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(CircleShape)
                            .background(
                                when (timerState) {
                                    is FocusTimerState.Running -> ArOsTheme.colors.surfaceRaised
                                    is FocusTimerState.Paused -> ArOsTheme.colors.warning.copy(alpha = 0.1f)
                                    is FocusTimerState.Completed -> ArOsTheme.colors.success.copy(alpha = 0.15f)
                                    is FocusTimerState.Idle -> ArOsTheme.colors.background
                                }
                            )
                            .border(
                                1.dp,
                                when (timerState) {
                                    is FocusTimerState.Running -> ArOsTheme.colors.accent.copy(alpha = 0.5f)
                                    is FocusTimerState.Paused -> ArOsTheme.colors.warning.copy(alpha = 0.5f)
                                    is FocusTimerState.Completed -> ArOsTheme.colors.success.copy(alpha = 0.5f)
                                    is FocusTimerState.Idle -> ArOsTheme.colors.hairline
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (timerState) {
                                    is FocusTimerState.Running -> "FLOW STATE ACTIVE"
                                    is FocusTimerState.Paused -> "SESSION PAUSED"
                                    is FocusTimerState.Completed -> "FLOW COMPLETED"
                                    is FocusTimerState.Idle -> "READY FOR DEEP FOCUS"
                                },
                                color = when (timerState) {
                                    is FocusTimerState.Completed -> ArOsTheme.colors.success
                                    is FocusTimerState.Paused -> ArOsTheme.colors.warning
                                    else -> ArOsTheme.colors.accent
                                },
                                style = ArOsTheme.typography.caption
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = timeFormatted,
                                style = ArOsTheme.typography.numericDisplay,
                                color = ArOsTheme.colors.contentPrimary,
                                modifier = Modifier.testTag("focus_timer_text")
                            )

                            Text(
                                text = activeMode.label,
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }
                    }
                }
            }

            // 3. Timer Control Actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ArOsTheme.spacing.screenMargin),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (timerState) {
                        is FocusTimerState.Idle, is FocusTimerState.Completed -> {
                            ArOsButton(
                                text = "Begin Focus Block",
                                onClick = onStart,
                                variant = ArOsButtonVariant.Primary,
                                leadingIcon = Icons.Default.PlayArrow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("start_focus_button")
                            )
                        }
                        is FocusTimerState.Running -> {
                            ArOsButton(
                                text = "Pause",
                                onClick = onPause,
                                variant = ArOsButtonVariant.Secondary,
                                leadingIcon = Icons.Default.Pause,
                                modifier = Modifier.weight(1f)
                            )
                            ArOsButton(
                                text = "Reset",
                                onClick = onCancel,
                                variant = ArOsButtonVariant.Ghost,
                                leadingIcon = Icons.Default.Refresh,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        is FocusTimerState.Paused -> {
                            ArOsButton(
                                text = "Resume",
                                onClick = onResume,
                                variant = ArOsButtonVariant.Primary,
                                leadingIcon = Icons.Default.PlayArrow,
                                modifier = Modifier.weight(1f)
                            )
                            ArOsButton(
                                text = "Reset",
                                onClick = onCancel,
                                variant = ArOsButtonVariant.Ghost,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Interruption Logger during active session
            if (timerState is FocusTimerState.Running || timerState is FocusTimerState.Paused) {
                item {
                    ArOsButton(
                        text = "Record Distraction / Interruption",
                        onClick = onLogInterruption,
                        variant = ArOsButtonVariant.Secondary,
                        leadingIcon = Icons.Default.Warning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ArOsTheme.spacing.screenMargin)
                    )
                }
            }

            // 5. Daily Focus Mastery Telemetry Card
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(ArOsTheme.shapes.sm)
                                    .background(ArOsTheme.colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Focus",
                                    tint = ArOsTheme.colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "TODAY'S DEEP FOCUS",
                                    style = ArOsTheme.typography.caption,
                                    color = ArOsTheme.colors.contentTertiary
                                )
                                Text(
                                    text = "${todayFocusMinutes} minutes",
                                    style = ArOsTheme.typography.numericTitle,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                            }
                        }

                        Text(
                            text = if (todayFocusMinutes >= 90) "Goal Exceeded" else "${(90 - todayFocusMinutes).coerceAtLeast(0)}m to goal",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.accent
                        )
                    }
                }
            }
        }
    }
}
