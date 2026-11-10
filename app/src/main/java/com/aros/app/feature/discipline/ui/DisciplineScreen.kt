package com.aros.app.feature.discipline.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsEmptyState
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsIconButton
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.haptics.rememberArOsHaptics
import com.aros.app.designsystem.theme.ArOsTheme
import com.aros.app.feature.discipline.domain.DisciplineChallengeType
import com.aros.app.feature.discipline.domain.DisciplineRule
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineScreen(
    rules: List<DisciplineRule>,
    onToggleRule: (DisciplineRule) -> Unit,
    onAddRule: (String, DisciplineChallengeType, Int, String) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onRecordChallengePass: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var activeChallengeRule by remember { mutableStateOf<DisciplineRule?>(null) }
    val haptics = rememberArOsHaptics()

    val totalPassed = rules.sumOf { it.passCount }

    ArOsScreenScaffold(
        title = "Friction Gates",
        actions = {
            ArOsIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Add Gate",
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_discipline_rule_button")
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
            // 1. Friction Overview Card
            item {
                ArOsCard(
                    modifier = Modifier
                        .padding(horizontal = ArOsTheme.spacing.screenMargin)
                        .testTag("discipline_summary_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COGNITIVE FRICTION GATES",
                                style = ArOsTheme.typography.caption,
                                color = ArOsTheme.colors.contentTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalPassed Overcome",
                                style = ArOsTheme.typography.numericTitle,
                                color = ArOsTheme.colors.success
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${rules.count { it.isActive }} active resistance barriers",
                                style = ArOsTheme.typography.footnote,
                                color = ArOsTheme.colors.contentSecondary
                            )
                        }

                        ArOsButton(
                            text = "Add Gate",
                            onClick = { showAddDialog = true },
                            variant = ArOsButtonVariant.Secondary,
                            leadingIcon = Icons.Default.Add
                        )
                    }
                }
            }

            // 2. Rules List or Empty State
            if (rules.isEmpty()) {
                item {
                    ArOsEmptyState(
                        icon = Icons.Default.Shield,
                        title = "No Cognitive Friction Active",
                        description = "Insert deliberate conscious pauses, breath barriers, or math gates before compulsive phone opening.",
                        actionButtonText = "Create First Gate",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                item {
                    ArOsGroup(
                        header = "Configured Resistance Gates",
                        footer = "Gates introduce prefrontal pause barriers before addictive application triggers."
                    ) {
                        rules.forEachIndexed { index, rule ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = ArOsTheme.spacing.md,
                                        vertical = ArOsTheme.spacing.sm
                                    )
                                    .testTag("discipline_rule_${rule.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = rule.challengeType.label.uppercase(),
                                                style = ArOsTheme.typography.caption,
                                                color = Color(0xFFA78BFA)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${rule.passCount} passed",
                                                style = ArOsTheme.typography.numericBody,
                                                color = ArOsTheme.colors.success
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = rule.name,
                                            style = ArOsTheme.typography.headline,
                                            color = if (rule.isActive) ArOsTheme.colors.contentPrimary else ArOsTheme.colors.contentTertiary
                                        )
                                        Text(
                                            text = "Target: ${rule.targetAppOrHabit}",
                                            style = ArOsTheme.typography.footnote,
                                            color = ArOsTheme.colors.contentSecondary
                                        )
                                    }

                                    Switch(
                                        checked = rule.isActive,
                                        onCheckedChange = {
                                            haptics.tick()
                                            onToggleRule(rule)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = ArOsTheme.colors.accent,
                                            uncheckedThumbColor = ArOsTheme.colors.contentTertiary,
                                            uncheckedTrackColor = ArOsTheme.colors.surfaceRaised
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(ArOsTheme.spacing.sm))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ArOsButton(
                                        text = "Simulate Gate",
                                        onClick = { activeChallengeRule = rule },
                                        variant = ArOsButtonVariant.Secondary,
                                        leadingIcon = Icons.Default.PlayArrow,
                                        modifier = Modifier.testTag("test_gate_button_${rule.id}")
                                    )

                                    IconButton(
                                        onClick = { onDeleteRule(rule.id) },
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
    }

    // Interactive Challenge Modals
    activeChallengeRule?.let { rule ->
        when (rule.challengeType) {
            DisciplineChallengeType.BREATH_GATE -> {
                BreathGateDialog(
                    rule = rule,
                    onDismiss = { activeChallengeRule = null },
                    onComplete = {
                        haptics.success()
                        onRecordChallengePass(rule.id)
                        activeChallengeRule = null
                    }
                )
            }
            DisciplineChallengeType.PLEDGE_TYPING -> {
                PledgeTypingDialog(
                    rule = rule,
                    onDismiss = { activeChallengeRule = null },
                    onComplete = {
                        haptics.success()
                        onRecordChallengePass(rule.id)
                        activeChallengeRule = null
                    }
                )
            }
            DisciplineChallengeType.ARITHMETIC -> {
                ArithmeticChallengeDialog(
                    rule = rule,
                    onDismiss = { activeChallengeRule = null },
                    onComplete = {
                        haptics.success()
                        onRecordChallengePass(rule.id)
                        activeChallengeRule = null
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        var challengeType by remember { mutableStateOf(DisciplineChallengeType.BREATH_GATE) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Create Friction Gate",
                    style = ArOsTheme.typography.headline,
                    color = ArOsTheme.colors.contentPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Gate Name (e.g. Social Feed Barrier)") },
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
                            .testTag("new_discipline_name")
                    )

                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target Application or Impulsive Habit") },
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

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Challenge Type",
                        style = ArOsTheme.typography.caption,
                        color = ArOsTheme.colors.contentTertiary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DisciplineChallengeType.entries.forEach { type ->
                            FilterChip(
                                selected = challengeType == type,
                                onClick = { challengeType = type },
                                label = { Text(type.label, style = ArOsTheme.typography.caption) },
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
                    text = "Create Gate",
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddRule(name, challengeType, challengeType.defaultFrictionSeconds, target)
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

@Composable
private fun BreathGateDialog(
    rule: DisciplineRule,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(rule.frictionSeconds) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Conscious Breath Gate",
                style = ArOsTheme.typography.headline,
                color = ArOsTheme.colors.contentPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Breathe deeply and pause before accessing ${rule.targetAppOrHabit}.",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(75.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (secondsLeft > 0) "$secondsLeft" else "✓",
                            style = ArOsTheme.typography.numericDisplay,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (secondsLeft > 0) "Inhale... Exhale..." else "Pause Completed!",
                    style = ArOsTheme.typography.headline,
                    color = if (secondsLeft > 0) Color(0xFFA78BFA) else ArOsTheme.colors.success
                )
            }
        },
        containerColor = ArOsTheme.colors.surfaceOverlay,
        confirmButton = {
            ArOsButton(
                text = "Pass Gate",
                onClick = onComplete,
                enabled = secondsLeft == 0,
                variant = ArOsButtonVariant.Primary
            )
        },
        dismissButton = {
            ArOsButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = ArOsButtonVariant.Ghost
            )
        }
    )
}

@Composable
private fun PledgeTypingDialog(
    rule: DisciplineRule,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val pledgeTarget = "I choose intention over impulse"
    var typedText by remember { mutableStateOf("") }
    val isMatch = typedText.trim().equals(pledgeTarget, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Intention Pledge",
                style = ArOsTheme.typography.headline,
                color = ArOsTheme.colors.contentPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)) {
                Text(
                    text = "Type the commitment pledge to confirm deliberate choice:",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ArOsTheme.shapes.sm)
                        .background(ArOsTheme.colors.surfaceRaised)
                        .border(1.dp, ArOsTheme.colors.hairline, ArOsTheme.shapes.sm)
                        .padding(ArOsTheme.spacing.sm)
                ) {
                    Text(
                        text = "\"$pledgeTarget\"",
                        style = ArOsTheme.typography.callout,
                        color = ArOsTheme.colors.accent
                    )
                }

                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    label = { Text("Type pledge here") },
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
                        .testTag("pledge_input")
                )
            }
        },
        containerColor = ArOsTheme.colors.surfaceOverlay,
        confirmButton = {
            ArOsButton(
                text = "Unlock",
                onClick = onComplete,
                enabled = isMatch,
                variant = ArOsButtonVariant.Primary
            )
        },
        dismissButton = {
            ArOsButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = ArOsButtonVariant.Ghost
            )
        }
    )
}

@Composable
private fun ArithmeticChallengeDialog(
    rule: DisciplineRule,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val num1 = remember { (12..35).random() }
    val num2 = remember { (14..45).random() }
    val expected = num1 + num2
    var answerInput by remember { mutableStateOf("") }
    val isCorrect = answerInput.trim() == expected.toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Mental Focus Barrier",
                style = ArOsTheme.typography.headline,
                color = ArOsTheme.colors.contentPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ArOsTheme.spacing.sm)) {
                Text(
                    text = "Engage your prefrontal focus before proceeding:",
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentSecondary
                )
                Text(
                    text = "$num1 + $num2 = ?",
                    style = ArOsTheme.typography.numericDisplay,
                    color = ArOsTheme.colors.accent
                )
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = { answerInput = it },
                    label = { Text("Answer") },
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
                        .testTag("math_input")
                )
            }
        },
        containerColor = ArOsTheme.colors.surfaceOverlay,
        confirmButton = {
            ArOsButton(
                text = "Verify & Pass",
                onClick = onComplete,
                enabled = isCorrect,
                variant = ArOsButtonVariant.Primary
            )
        },
        dismissButton = {
            ArOsButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = ArOsButtonVariant.Ghost
            )
        }
    )
}
