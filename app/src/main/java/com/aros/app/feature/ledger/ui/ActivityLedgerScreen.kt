package com.aros.app.feature.ledger.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsEmptyState
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsIconButton
import com.aros.app.designsystem.components.ArOsPillTone
import com.aros.app.designsystem.components.ArOsRow
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.ledger.data.ActivityLedgerEntity
import com.aros.app.feature.ledger.domain.DailyLedgerSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLedgerScreen(
    summary: DailyLedgerSummary,
    entries: List<ActivityLedgerEntity>,
    onLogIntentionalEvent: (String, Long) -> Unit
) {
    var showLogDialog by remember { mutableStateOf(false) }

    ArOsScreenScaffold(
        title = "Activity Ledger",
        actions = {
            ArOsIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Log Conscious Action",
                onClick = { showLogDialog = true },
                modifier = Modifier.testTag("log_conscious_action_button")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)
        ) {
            // 1. Daily Activity Sovereignty Summary Card
            item {
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("ledger_summary_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PHONE INTENTIONALITY",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${summary.intentionalRatioPercent}%",
                                style = ArOsTheme.typography.numericDisplay,
                                color = if (summary.intentionalRatioPercent >= 70) ArOsTheme.colors.success else ArOsTheme.colors.warning
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${summary.totalPhoneUnlocks} device opens recorded today",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        ArOsButton(
                            text = "Log Action",
                            onClick = { showLogDialog = true },
                            variant = ArOsButtonVariant.Secondary,
                            leadingIcon = Icons.Default.Add
                        )
                    }
                }
            }

            // 2. 24-Hour Activity Density Histogram
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
                                text = "24-Hour Activity Density",
                                style = ArOsTheme.typography.headline,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Text(
                                text = "00:00 — 23:59",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

                        val maxCount = (summary.hourlyActivityBuckets.maxOrNull() ?: 1).coerceAtLeast(1)
                        val buckets = summary.hourlyActivityBuckets
                        val emptyColor = ArOsTheme.colors.surfaceRaised
                        val activeColor = ArOsTheme.colors.accent
                        val peakColor = ArOsTheme.colors.info

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .testTag("activity_histogram_canvas")
                        ) {
                            val widthPerBar = size.width / 24f
                            val barWidth = widthPerBar * 0.65f
                            val cornerRad = CornerRadius(4f, 4f)

                            for (i in 0 until 24) {
                                val count = if (i < buckets.size) buckets[i] else 0
                                val barHeight = ((count.toFloat() / maxCount.toFloat()) * size.height).coerceAtLeast(5f)
                                val x = (i * widthPerBar) + ((widthPerBar - barWidth) / 2f)
                                val y = size.height - barHeight

                                val barColor = if (count == 0) {
                                    emptyColor
                                } else if (count > (maxCount * 0.6f)) {
                                    peakColor
                                } else {
                                    activeColor
                                }

                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = cornerRad
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("12 AM", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentTertiary)
                            Text("6 AM", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentTertiary)
                            Text("12 PM", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentTertiary)
                            Text("6 PM", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentTertiary)
                            Text("11 PM", style = ArOsTheme.typography.caption, color = ArOsTheme.colors.contentTertiary)
                        }
                    }
                }
            }

            // 3. Activity Stream List
            if (entries.isEmpty()) {
                item {
                    ArOsEmptyState(
                        icon = Icons.Default.QueryStats,
                        title = "No Activity Logged Today",
                        description = "Device unlocks and conscious actions will populate here automatically.",
                        actionButtonText = "Log First Action",
                        onActionClick = { showLogDialog = true }
                    )
                }
            } else {
                item {
                    ArOsGroup(
                        header = "Activity Stream",
                        footer = "Sorted chronologically with tabular timestamps"
                    ) {
                        entries.forEachIndexed { index, entry ->
                            val timeStr = com.aros.app.core.TimeFormatter.formatClock(entry.timestamp)
                            ArOsRow(
                                title = entry.appLabel,
                                subtitle = "${entry.eventCategory} • ${if (entry.isIntentional) "Intentional" else "Impulsive"}",
                                leadingIcon = if (entry.isIntentional) Icons.Default.CheckCircle else Icons.Default.LockOpen,
                                leadingBadgeColor = if (entry.isIntentional) ArOsTheme.colors.success else ArOsTheme.colors.warning,
                                trailingContent = {
                                    Text(
                                        text = timeStr,
                                        style = ArOsTheme.typography.numericBody,
                                        color = ArOsTheme.colors.contentTertiary
                                    )
                                },
                                showDivider = index < entries.size - 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        var actionLabel by remember { mutableStateOf("") }
        var isIntentional by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = {
                Text(
                    text = "Log Conscious Activity",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.md)) {
                    OutlinedTextField(
                        value = actionLabel,
                        onValueChange = { actionLabel = it },
                        label = { Text("Activity (e.g. Deep Work, Reading)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArOsTheme.colors.accent,
                            unfocusedBorderColor = ArOsTheme.colors.hairline,
                            focusedTextColor = ArOsTheme.colors.contentPrimary,
                            unfocusedTextColor = ArOsTheme.colors.contentPrimary,
                            focusedContainerColor = ArOsTheme.colors.surfaceRaised,
                            unfocusedContainerColor = ArOsTheme.colors.surfaceRaised
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ledger_activity_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intentional & Planned:",
                            style = ArOsTheme.typography.subhead,
                            color = ArOsTheme.colors.contentSecondary
                        )
                        ArOsButton(
                            text = if (isIntentional) "Intentional" else "Impulsive",
                            onClick = { isIntentional = !isIntentional },
                            variant = if (isIntentional) ArOsButtonVariant.Primary else ArOsButtonVariant.Secondary
                        )
                    }
                }
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Save",
                    onClick = {
                        if (actionLabel.isNotBlank()) {
                            onLogIntentionalEvent(actionLabel, 300)
                            showLogDialog = false
                        }
                    },
                    variant = ArOsButtonVariant.Primary
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Cancel",
                    onClick = { showLogDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }
}
