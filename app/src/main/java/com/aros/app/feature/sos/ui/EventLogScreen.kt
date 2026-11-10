package com.aros.app.feature.sos.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.aros.app.designsystem.components.ArOsEmptyState
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsIconButton
import com.aros.app.designsystem.components.ArOsPillTone
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.sos.domain.SosEvent
import com.aros.app.feature.sos.domain.SosEventType
import com.aros.app.feature.sos.domain.SosMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLogScreen(
    events: List<SosEvent>,
    onClearLog: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    ArOsScreenScaffold(
        title = "Audit Log",
        actions = {
            if (events.isNotEmpty()) {
                ArOsIconButton(
                    icon = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Log",
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clear_log_button")
                )
            }
        }
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
                            text = "SECURITY AUDIT LOG",
                            style = ArOsTheme.typography.caption,
                            color = ArOsTheme.colors.contentTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hardware Event History",
                            style = ArOsTheme.typography.headline,
                            color = ArOsTheme.colors.contentPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tamper-evident, local audit log recording triggers, cancellations, and dial events. Auto-prunes at 500 records.",
                            style = ArOsTheme.typography.footnote,
                            color = ArOsTheme.colors.contentSecondary
                        )
                    }
                }
            }

            if (events.isEmpty()) {
                item {
                    ArOsEmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "No Recorded Events",
                        description = "Triggers, simulations, and cancellations will be logged here with microsecond precision."
                    )
                }
            } else {
                item {
                    ArOsGroup(header = "Logged Security Operations") {
                        events.forEachIndexed { index, event ->
                            val dateStr = com.aros.app.core.TimeFormatter.formatDateTime(event.timestamp)
                            val isLive = event.mode == SosMode.LIVE
                            val statusColor = when (event.type) {
                                SosEventType.TRIGGER_DETECTED, SosEventType.COUNTDOWN_STARTED -> ArOsTheme.colors.warning
                                SosEventType.DIAL_REQUESTED -> if (isLive) ArOsTheme.colors.danger else ArOsTheme.colors.accent
                                SosEventType.DIAL_FAILED, SosEventType.READINESS_WARNING, SosEventType.WARN_AIRPLANE_MODE -> ArOsTheme.colors.danger
                                SosEventType.CANCELLED -> ArOsTheme.colors.contentTertiary
                                SosEventType.TEST_NO_CALL -> ArOsTheme.colors.success
                                else -> ArOsTheme.colors.accent
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = ArOsTheme.spacing.md,
                                        vertical = ArOsTheme.spacing.sm
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = event.type.name,
                                            style = ArOsTheme.typography.headline,
                                            color = ArOsTheme.colors.contentPrimary
                                        )
                                    }

                                    ArOsStatusPill(
                                        label = event.mode.name,
                                        tone = if (isLive) ArOsPillTone.Danger else ArOsPillTone.Warning,
                                        showDot = false
                                    )
                                }

                                if (event.detail.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = event.detail,
                                        style = ArOsTheme.typography.footnote,
                                        color = ArOsTheme.colors.contentSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = dateStr,
                                        style = ArOsTheme.typography.numericBody,
                                        color = ArOsTheme.colors.contentTertiary
                                    )
                                    Text(
                                        text = if (event.deviceLocked) "Locked Screen" else "Unlocked",
                                        style = ArOsTheme.typography.caption,
                                        color = ArOsTheme.colors.contentTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Clear Event Audit Log?",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently purge all recorded trigger and calling logs on this device.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Clear All Entries",
                    onClick = {
                        showClearDialog = false
                        onClearLog()
                    },
                    variant = ArOsButtonVariant.Destructive
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Cancel",
                    onClick = { showClearDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }
}
