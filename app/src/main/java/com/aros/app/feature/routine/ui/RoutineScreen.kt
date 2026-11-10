package com.aros.app.feature.routine.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.aros.app.designsystem.components.ArOsRow
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.haptics.rememberArOsHaptics
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.routine.domain.RoutineCategory
import com.aros.app.feature.routine.domain.RoutineItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    routines: List<RoutineItem>,
    onToggleCompletion: (RoutineItem) -> Unit,
    onAddRoutine: (String, String, Int, Int, RoutineCategory) -> Unit,
    onDeleteRoutine: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val haptics = rememberArOsHaptics()

    val completedCount = routines.count { it.completedToday }
    val totalCount = routines.size
    val completionPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

    ArOsScreenScaffold(
        title = "Rhythms",
        actions = {
            ArOsIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add Routine",
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_routine_button")
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
            // 1. Daily Rhythm Adherence Summary
            item {
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("routine_summary_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DAILY RHYTHM & HABITS",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedCount of $totalCount Done",
                                style = ArOsTheme.typography.numericTitle,
                                color = ArOsTheme.colors.contentPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$completionPercent% adherence today",
                                style = ArOsTheme.typography.footnote,
                                color = if (completionPercent >= 75) ArOsTheme.colors.success else ArOsTheme.colors.accent
                            )
                        }

                        ArOsButton(
                            text = "New Routine",
                            onClick = { showAddDialog = true },
                            variant = ArOsButtonVariant.Secondary,
                            leadingIcon = Icons.Default.Add
                        )
                    }
                }
            }

            // 2. Routines Inset Group or Empty State
            if (routines.isEmpty()) {
                item {
                    ArOsEmptyState(
                        icon = Icons.Default.Schedule,
                        title = "No Daily Rhythms Configured",
                        description = "Establish healthy anchors for your morning, deep focus, and evening winding down.",
                        actionButtonText = "Create First Rhythm",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                item {
                    ArOsGroup(
                        header = "Active Routines",
                        footer = "Tap circle to toggle completion. Streaks track consecutive daily adherence."
                    ) {
                        routines.forEachIndexed { index, routine ->
                            val categoryColor = when (routine.category) {
                                RoutineCategory.MORNING -> ArOsTheme.colors.warning
                                RoutineCategory.FOCUS -> ArOsTheme.colors.accent
                                RoutineCategory.HEALTH -> ArOsTheme.colors.success
                                RoutineCategory.EVENING -> Color(0xFF8B5CF6)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.tick()
                                        onToggleCompletion(routine)
                                    }
                                    .padding(
                                        horizontal = ArOsTheme.spacing.md,
                                        vertical = ArOsTheme.spacing.sm
                                    )
                                    .testTag("routine_item_${routine.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Completion Toggle Box
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (routine.completedToday) ArOsTheme.colors.success else ArOsTheme.colors.surfaceRaised
                                        )
                                        .border(
                                            1.dp,
                                            if (routine.completedToday) ArOsTheme.colors.success else ArOsTheme.colors.hairline,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (routine.completedToday) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(ArOsTheme.spacing.md))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = routine.category.label.uppercase(),
                                            style = ArOsTheme.typography.caption,
                                            color = categoryColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = routine.formattedTime,
                                            style = ArOsTheme.typography.numericBody,
                                            color = ArOsTheme.colors.contentTertiary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = routine.title,
                                        style = ArOsTheme.typography.body,
                                        color = if (routine.completedToday)
                                            ArOsTheme.colors.contentTertiary
                                        else
                                            ArOsTheme.colors.contentPrimary
                                    )

                                    if (routine.description.isNotBlank()) {
                                        Text(
                                            text = routine.description,
                                            style = ArOsTheme.typography.footnote,
                                            color = ArOsTheme.colors.contentSecondary
                                        )
                                    }
                                }

                                if (routine.streakCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(ArOsTheme.shapes.pill)
                                            .background(ArOsTheme.colors.warning.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = "Streak",
                                            tint = ArOsTheme.colors.warning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${routine.streakCount}d",
                                            style = ArOsTheme.typography.caption,
                                            color = ArOsTheme.colors.warning
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteRoutine(routine.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = ArOsTheme.colors.contentTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(RoutineCategory.MORNING) }
        var selectedHour12 by remember { mutableIntStateOf(8) }
        var isPm by remember { mutableStateOf(false) }
        var durationMins by remember { mutableIntStateOf(20) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Create Routine",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Routine Title") },
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
                            .testTag("new_routine_title")
                    )

                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Short Purpose / Action") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArOsTheme.colors.accent,
                            unfocusedBorderColor = ArOsTheme.colors.hairline,
                            focusedTextColor = ArOsTheme.colors.contentPrimary,
                            unfocusedTextColor = ArOsTheme.colors.contentPrimary,
                            focusedContainerColor = ArOsTheme.colors.surfaceRaised,
                            unfocusedContainerColor = ArOsTheme.colors.surfaceRaised
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scheduled Time (12-Hour)",
                        style = ArOsTheme.typography.caption,
                        color = ArOsTheme.colors.contentTertiary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(6, 8, 9, 12, 2, 5, 9).forEach { h ->
                                FilterChip(
                                    selected = selectedHour12 == h,
                                    onClick = { selectedHour12 = h },
                                    label = { Text("$h:00", style = ArOsTheme.typography.caption) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = !isPm,
                                onClick = { isPm = false },
                                label = { Text("AM", style = ArOsTheme.typography.caption) }
                            )
                            FilterChip(
                                selected = isPm,
                                onClick = { isPm = true },
                                label = { Text("PM", style = ArOsTheme.typography.caption) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Category",
                        style = ArOsTheme.typography.caption,
                        color = ArOsTheme.colors.contentTertiary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoutineCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label, style = ArOsTheme.typography.caption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArOsTheme.colors.accent,
                                    selectedLabelColor = ArOsTheme.colors.contentOnAccent
                                )
                            )
                        }
                    }
                }
            },
            containerColor = ArOsTheme.colors.surfaceOverlay,
            confirmButton = {
                ArOsButton(
                    text = "Save Routine",
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val hour24 = when {
                                selectedHour12 == 12 && !isPm -> 0
                                selectedHour12 == 12 && isPm -> 12
                                isPm -> selectedHour12 + 12
                                else -> selectedHour12
                            }
                            onAddRoutine(newTitle, newDesc, hour24 * 60, durationMins, selectedCategory)
                            showAddDialog = false
                        }
                    },
                    variant = ArOsButtonVariant.Primary
                )
            },
            dismissButton = {
                ArOsButton(
                    text = "Cancel",
                    onClick = { showAddDialog = false },
                    variant = ArOsButtonVariant.Ghost
                )
            }
        )
    }
}
