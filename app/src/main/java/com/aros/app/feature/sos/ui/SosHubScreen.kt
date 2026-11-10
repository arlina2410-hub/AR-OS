package com.aros.app.feature.sos.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosHubScreen(
    config: SosConfig,
    readinessReport: ReadinessReport?,
    primaryContact: SosContact?,
    availableSims: List<SimAccountInfo>,
    onSetMode: (SosMode) -> Unit,
    onTestNow: () -> Unit,
    onPlaceRealTestCall: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLab: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    var showLiveConfirmDialog by remember { mutableStateOf(false) }
    var showRealCallConfirmDialog by remember { mutableStateOf(false) }

    val isLive = config.mode == SosMode.LIVE
    val isReady = readinessReport?.overallStatus != OverallReadinessStatus.NEEDS_ATTENTION
    val activeSim = availableSims.find { it.id == config.selectedSimId } ?: availableSims.find { it.isDefault } ?: availableSims.firstOrNull()

    ArOsScreenScaffold(
        title = "Safety SOS",
        subtitle = "Offline hardware emergency trigger and trusted contacts"
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ArOsTheme.spacing.screenMargin)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. Prominent TEST ⇄ LIVE Switch Card
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
                                    text = if (isLive) "TEST: everything works except dialing. LIVE: the app really calls."
                                    else "TEST: everything works except dialing. LIVE: the app really calls.",
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
                                )
                            )
                        }
                    }
                }
            }

            // 2. Status Card
            item {
                ArOsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        if (isReady) (if (isLive) ArOsTheme.colors.danger else ArOsTheme.colors.success) else ArOsTheme.colors.warning,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isReady) "SOS is ON — ${if (isLive) "LIVE mode" else "TEST mode"}" else "SOS Needs Action",
                                    style = ArOsTheme.typography.body,
                                    color = ArOsTheme.colors.contentPrimary
                                )
                                Text(
                                    text = if (isReady) "Five Volume Down presses will trigger" else "Tap Permissions to resolve issues",
                                    style = ArOsTheme.typography.caption,
                                    color = ArOsTheme.colors.contentSecondary
                                )
                            }
                        }

                        if (!isReady) {
                            ArOsButton(
                                text = "Fix",
                                onClick = onNavigateToPermissions,
                                variant = ArOsButtonVariant.Primary
                            )
                        }
                    }
                }
            }

            // 3. Operational Summary Group (Primary Contact, SIM, Countdown)
            item {
                ArOsGroup(header = "Emergency Routing") {
                    // Primary Contact Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToContacts() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContactPhone, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Primary Contact", style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                                Text(primaryContact?.name ?: "None configured", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentSecondary)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
                    }

                    // SIM Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPermissions() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SimCard, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("SOS Calling SIM", style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                                Text(activeSim?.label ?: "Auto-default SIM", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentSecondary)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
                    }

                    // Countdown Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSettings() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Countdown Duration", style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                                Text(if (config.countdownSeconds == 0) "Immediate (0s)" else "${config.countdownSeconds} seconds cancellation window", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentSecondary)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 4. Action Buttons (Test Now + Real Live Call Test)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArOsButton(
                        text = "Test Now",
                        onClick = onTestNow,
                        variant = ArOsButtonVariant.Secondary,
                        leadingIcon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f)
                    )

                    if (isLive) {
                        ArOsButton(
                            text = "Place Real Test Call",
                            onClick = { showRealCallConfirmDialog = true },
                            variant = ArOsButtonVariant.CriticalSos,
                            leadingIcon = Icons.Default.Phone,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Secondary Tools and Info Rows
            item {
                ArOsGroup(header = "Safety Tools & Documentation") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToHistory() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("SOS History", style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToLab() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Trigger Lab", style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToHelp() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("What SOS Can't Do", style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSettings() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = ArOsTheme.colors.accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Safety Settings", style = ArOsTheme.typography.body, color = ArOsTheme.colors.contentPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ArOsTheme.colors.contentTertiary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showLiveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLiveConfirmDialog = false },
            title = { Text("Switch to LIVE Mode?", style = ArOsTheme.typography.headline, color = ArOsTheme.colors.contentPrimary) },
            text = {
                Text(
                    text = "In LIVE mode, five rapid Volume Down presses will place an ACTUAL cellular call to ${primaryContact?.name ?: "your primary contact"}.\n\nOnly enable this after confirming your trigger in TEST mode.",
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

    if (showRealCallConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRealCallConfirmDialog = false },
            title = { Text("Place Real Phone Call Now?", style = ArOsTheme.typography.headline, color = ArOsTheme.colors.contentPrimary) },
            text = {
                Text(
                    text = "This will immediately place a real cellular call to ${primaryContact?.name ?: "your primary contact"}.\n\nEnsure your contact knows you are conducting a verification test.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Place Call Now",
                    onClick = {
                        showRealCallConfirmDialog = false
                        onPlaceRealTestCall()
                    },
                    variant = ArOsButtonVariant.CriticalSos
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Cancel",
                    onClick = { showRealCallConfirmDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }
}
