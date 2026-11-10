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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.OverallReadinessStatus
import com.aros.app.feature.sos.domain.ReadinessReport
import com.aros.app.feature.sos.domain.SosConfig
import com.aros.app.feature.sos.domain.SosContact
import com.aros.app.feature.sos.domain.SosMode
import com.aros.app.feature.sos.platform.SimAccountInfo
import com.aros.app.shell.AppDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyHubScreen(
    config: SosConfig,
    readinessReport: ReadinessReport?,
    primaryContact: SosContact?,
    availableSims: List<SimAccountInfo>,
    onSetMode: (SosMode) -> Unit,
    onTestSosNow: () -> Unit,
    onPlaceRealTestCallNow: () -> Unit,
    onNavigateTo: (AppDestination) -> Unit
) {
    val isLive = config.mode == SosMode.LIVE
    val isReady = readinessReport?.overallStatus != OverallReadinessStatus.NEEDS_ATTENTION
    var showLiveConfirmDialog by remember { mutableStateOf(false) }
    var showRealCallDialog by remember { mutableStateOf(false) }

    val resolvedSim = availableSims.find { it.id == config.selectedSimId }
        ?: availableSims.find { it.isDefault }
        ?: availableSims.firstOrNull()

    ArOsScreenScaffold(
        title = "Safety SOS",
        subtitle = "Emergency calling hub and trigger management"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ArOsTheme.spacing.screenMargin)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. Prominent TEST ⇄ LIVE Switch Card at top
            item {
                ArOsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isLive) "LIVE EMERGENCY MODE" else "SAFE TEST MODE",
                                    style = ArOsTheme.typography.headline,
                                    color = if (isLive) ArOsTheme.colors.danger else ArOsTheme.colors.warning
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isLive) "LIVE: the app really calls your contact upon 5x press."
                                    else "TEST: everything works except dialing (zero calls placed).",
                                    style = ArOsTheme.typography.caption,
                                    color = ArOsTheme.colors.contentSecondary
                                )
                            }

                            Switch(
                                checked = isLive,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showLiveConfirmDialog = true
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
                                modifier = Modifier.testTag("safety_mode_toggle")
                            )
                        }
                    }
                }
            }

            // 2. SOS Status & Primary Setup Row
            item {
                ArOsGroup(header = "Emergency Dispatch Target") {
                    // Primary Contact Row
                    SafetyDetailRow(
                        title = "Primary Guardian",
                        value = primaryContact?.name ?: "None configured (Tap to add)",
                        icon = Icons.Default.ContactPhone,
                        onClick = { onNavigateTo(AppDestination.SOS_CONTACTS) }
                    )

                    // Calling SIM Row
                    SafetyDetailRow(
                        title = "Calling SIM Card",
                        value = resolvedSim?.let { "${it.label} (Slot ${it.slotIndex + 1})" } ?: "Auto-detecting...",
                        icon = Icons.Default.SimCard,
                        onClick = { onNavigateTo(AppDestination.PERMISSIONS) }
                    )

                    // Countdown Setting Row
                    SafetyDetailRow(
                        title = "Countdown Window",
                        value = if (config.countdownSeconds == 0) "Immediate (0s)" else "${config.countdownSeconds} seconds",
                        icon = Icons.Default.Speed,
                        onClick = { onNavigateTo(AppDestination.SOS_SETTINGS) }
                    )
                }
            }

            // 3. Testing Actions
            item {
                ArOsGroup(header = "Verification & Testing") {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ArOsButton(
                                text = "Test Trigger Now",
                                onClick = onTestSosNow,
                                variant = ArOsButtonVariant.Primary,
                                leadingIcon = Icons.Default.PlayArrow,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Visible ONLY in LIVE mode: Place one real test call now
                        if (isLive && primaryContact != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ArOsButton(
                                text = "Place One Real Test Call Now",
                                onClick = { showRealCallDialog = true },
                                variant = ArOsButtonVariant.CriticalSos,
                                leadingIcon = Icons.Default.Call,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 4. Secondary Hub Navigation Rows
            item {
                ArOsGroup(header = "Tools & Safety Guidelines") {
                    SafetyNavRow(
                        title = "SOS History",
                        subtitle = "Audit log of all countdowns, triggers, and events",
                        icon = Icons.Default.History,
                        onClick = { onNavigateTo(AppDestination.SOS_LOG) }
                    )
                    SafetyNavRow(
                        title = "Trigger Lab",
                        subtitle = "Test hardware Volume Down speed and bounce limits",
                        icon = Icons.Default.Speed,
                        onClick = { onNavigateTo(AppDestination.SOS_LAB) }
                    )
                    SafetyNavRow(
                        title = "How SOS Works",
                        subtitle = "Offline 5x volume key press algorithm explained",
                        icon = Icons.Default.HelpOutline,
                        onClick = { onNavigateTo(AppDestination.SOS_HELP) }
                    )
                    SafetyNavRow(
                        title = "What SOS Can't Do",
                        subtitle = "Limits, hardware boundaries, and emergency disclaimer",
                        icon = Icons.Default.HelpOutline,
                        onClick = { onNavigateTo(AppDestination.SOS_HELP) }
                    )
                    SafetyNavRow(
                        title = "Safety Settings",
                        subtitle = "Sensitivity presets, volume restorer, speakerphone",
                        icon = Icons.Default.Settings,
                        onClick = { onNavigateTo(AppDestination.SOS_SETTINGS) }
                    )
                }
            }
        }
    }

    // Live mode confirmation dialog
    if (showLiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLiveConfirmDialog = false },
            title = {
                Text(
                    text = "Switch to LIVE Mode?",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Text(
                    text = "In LIVE mode, five rapid Volume Down presses WILL place an actual cellular call to ${primaryContact?.name ?: "your primary contact"}.\n\nAlways ensure you have tested simulation in TEST mode first.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Enable LIVE Mode",
                    onClick = {
                        showLiveConfirmDialog = false
                        onSetMode(SosMode.LIVE)
                    },
                    variant = ArOsButtonVariant.CriticalSos
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Keep Safe TEST",
                    onClick = { showLiveConfirmDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }

    // Real call confirmation dialog
    if (showRealCallDialog) {
        AlertDialog(
            onDismissRequest = { showRealCallDialog = false },
            title = {
                Text(
                    text = "Place Real Call Now?",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Text(
                    text = "This will immediately place a real cellular phone call to ${primaryContact?.name} over SIM ${resolvedSim?.label ?: ""}.\n\nUse this once to verify carrier connectivity.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Call ${primaryContact?.name ?: "Now"}",
                    onClick = {
                        showRealCallDialog = false
                        onPlaceRealTestCallNow()
                    },
                    variant = ArOsButtonVariant.CriticalSos
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Cancel",
                    onClick = { showRealCallDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }
}

@Composable
private fun SafetyDetailRow(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentTertiary)
                Text(text = value, style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
            }
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SafetyNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                Text(text = subtitle, style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentSecondary)
            }
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
    }
}
