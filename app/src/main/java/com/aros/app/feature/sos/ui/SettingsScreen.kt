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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsSegmentedControl
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.SosConfig
import com.aros.app.feature.sos.domain.SosMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: SosConfig,
    onSetMode: (SosMode) -> Unit,
    onSetCountdown: (Int) -> Unit,
    onSetSensitivity: (String) -> Unit,
    onSetRestoreVolumeAfterSos: (Boolean) -> Unit,
    onSetStartCallOnSpeaker: (Boolean) -> Unit,
    onSetShowStatusNotification: (Boolean) -> Unit,
    onSetUse24HourTime: (Boolean) -> Unit
) {
    var showLiveWarningDialog by remember { mutableStateOf(false) }

    ArOsScreenScaffold(
        title = "Safety Settings"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. TEST vs LIVE Mode Card
            item {
                val isLive = config.mode == SosMode.LIVE
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("mode_switch_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isLive) "LIVE EMERGENCY MODE" else "TEST MODE (SAFE)",
                                    style = ArOsTheme.typography.headline,
                                    color = if (isLive) ArOsTheme.colors.danger else ArOsTheme.colors.warning
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isLive) "Real cellular phone calls WILL be placed upon countdown completion."
                                    else "Safe test simulation only. Zero actual cellular calls placed.",
                                    style = ArOsTheme.typography.footnote,
                                    color = ArOsTheme.colors.contentSecondary
                                )
                            }

                            Switch(
                                checked = isLive,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showLiveWarningDialog = true
                                    } else {
                                        onSetMode(SosMode.TEST)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ArOsTheme.colors.danger,
                                    uncheckedThumbColor = ArOsTheme.colors.contentTertiary,
                                    uncheckedTrackColor = ArOsTheme.colors.surfaceRaised
                                ),
                                modifier = Modifier.testTag("mode_toggle_switch")
                            )
                        }
                    }
                }
            }

            // 2. Countdown Duration Slider (0..10s)
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Countdown Duration",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Text(
                                text = if (config.countdownSeconds == 0) "0s (Immediate)" else "${config.countdownSeconds}s",
                                style = ArOsTheme.typography.numericTitle,
                                color = ArOsTheme.colors.accent
                            )
                        }
                        Text(
                            text = "Cancellation window after 5 rapid presses before dialing initiates (0-10s).",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )
                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.sm))
                        Slider(
                            value = config.countdownSeconds.toFloat(),
                            onValueChange = { onSetCountdown(it.toInt()) },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = ArOsTheme.colors.accent,
                                activeTrackColor = ArOsTheme.colors.accent,
                                inactiveTrackColor = ArOsTheme.colors.surfaceRaised
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("countdown_slider")
                        )
                    }
                }
            }

            // 3. Hardware Trigger Sensitivity
            item {
                ArOsCard(
                    modifier = Modifier.padding(horizontal = ArOsTheme.spacing.screenMargin)
                ) {
                    Column {
                        Text(
                            text = "Hardware Key Sensitivity",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Text(
                            text = "Time allowed for all five presses (Relaxed: 3.5s, Normal: 2.5s, Strict: 1.8s).",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )
                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        val presets = listOf("Relaxed", "Normal", "Strict")
                        val selectedIdx = when (config.sensitivityPreset.uppercase()) {
                            "RELAXED" -> 0
                            "STRICT" -> 2
                            else -> 1
                        }

                        ArOsSegmentedControl(
                            options = presets,
                            selectedIndex = selectedIdx,
                            onOptionSelected = { idx ->
                                onSetSensitivity(presets[idx].uppercase())
                            }
                        )
                    }
                }
            }

            // 4. Hardware and Call Settings Toggles
            item {
                ArOsGroup(header = "Operational Behavior") {
                    // Restore Volume Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = ArOsTheme.spacing.md,
                                vertical = ArOsTheme.spacing.sm
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Restore volume after SOS",
                                style = ArOsTheme.typography.body,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Text(
                                text = "Reverts media and ring volume lowered by key presses during trigger.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        Switch(
                            checked = config.restoreVolumeAfterSos,
                            onCheckedChange = { onSetRestoreVolumeAfterSos(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ArOsTheme.colors.accent,
                                uncheckedThumbColor = ArOsTheme.colors.contentTertiary,
                                uncheckedTrackColor = ArOsTheme.colors.surfaceRaised
                            )
                        )
                    }

                    // Start Call on Speaker Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = ArOsTheme.spacing.md,
                                vertical = ArOsTheme.spacing.sm
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start call on speaker",
                                style = ArOsTheme.typography.body,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Text(
                                text = "Requests speakerphone when dialing primary contact.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        Switch(
                            checked = config.startCallOnSpeaker,
                            onCheckedChange = { onSetStartCallOnSpeaker(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ArOsTheme.colors.accent,
                                uncheckedThumbColor = ArOsTheme.colors.contentTertiary,
                                uncheckedTrackColor = ArOsTheme.colors.surfaceRaised
                            )
                        )
                    }

                    // Show Status Notification Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = ArOsTheme.spacing.md,
                                vertical = ArOsTheme.spacing.sm
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show status notification",
                                style = ArOsTheme.typography.body,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Text(
                                text = "Displays persistent status notification showing mode and readiness.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        Switch(
                            checked = config.showStatusNotification,
                            onCheckedChange = { onSetShowStatusNotification(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ArOsTheme.colors.accent,
                                uncheckedThumbColor = ArOsTheme.colors.contentTertiary,
                                uncheckedTrackColor = ArOsTheme.colors.surfaceRaised
                            )
                        )
                    }

                    // Time format option: 12-hour (default) / 24-hour
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = ArOsTheme.spacing.md,
                                vertical = ArOsTheme.spacing.sm
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Time format",
                                style = ArOsTheme.typography.body,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Text(
                                text = "12-hour with AM/PM (default) or 24-hour clock.",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        ArOsSegmentedControl(
                            options = listOf("12-Hour", "24-Hour"),
                            selectedIndex = if (config.use24HourTime) 1 else 0,
                            onOptionSelected = { idx ->
                                onSetUse24HourTime(idx == 1)
                            }
                        )
                    }
                }
            }

            // 5. Disclaimer Notice
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .clip(ArOsTheme.shapes.md)
                        .background(ArOsTheme.colors.surfaceRaised)
                        .border(1.dp, ArOsTheme.colors.hairline, ArOsTheme.shapes.md)
                        .padding(ArOsTheme.spacing.md)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ArOsTheme.colors.warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(ArOsTheme.spacing.sm))
                        Text(
                            text = "AR SOS is an offline convenience tool and does not guarantee cellular carrier connectivity or certified emergency service dispatch. Never disable your phone's built-in Emergency SOS.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary,
                            lineHeight = ArOsTheme.typography.footnote.lineHeight
                        )
                    }
                }
            }
        }
    }

    if (showLiveWarningDialog) {
        AlertDialog(
            onDismissRequest = { showLiveWarningDialog = false },
            title = {
                Text(
                    text = "Switch to LIVE Mode?",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Text(
                    text = "In LIVE mode, five rapid Volume Down presses will place an ACTUAL cellular call to your primary contact upon countdown completion.\n\nOnly switch to LIVE mode after successfully verifying safe TEST mode simulation.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Enable LIVE Mode",
                    onClick = {
                        showLiveWarningDialog = false
                        onSetMode(SosMode.LIVE)
                    },
                    variant = ArOsButtonVariant.CriticalSos
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Keep Safe TEST",
                    onClick = { showLiveWarningDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }
}
