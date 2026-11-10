package com.aros.app.designsystem.gallery

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.components.ArOsButton
import com.aros.app.designsystem.components.ArOsButtonVariant
import com.aros.app.designsystem.components.ArOsCard
import com.aros.app.designsystem.components.ArOsGroup
import com.aros.app.designsystem.components.ArOsIconButton
import com.aros.app.designsystem.components.ArOsPillTone
import com.aros.app.designsystem.components.ArOsRow
import com.aros.app.designsystem.components.ArOsScreenScaffold
import com.aros.app.designsystem.components.ArOsSegmentedControl
import com.aros.app.designsystem.components.ArOsStatusPill
import com.aros.app.designsystem.theme.ArOsTheme

/**
 * Visual verification gallery for the AR/OS Design System.
 * Tests tokens, typography, tabular numeral alignment, components, and interactive haptics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArOsDesignGallery(
    onBack: () -> Unit
) {
    var selectedSegment by remember { mutableIntStateOf(0) }

    ArOsScreenScaffold(
        title = "Design Gallery",
        navigationIcon = {
            ArOsIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp)
        ) {
            // Section 1: Tabular figures alignment test
            ArOsGroup(
                header = "Tabular Numeral Alignment (tnum test)",
                footer = "Numbers 00:00:00 and 11:11:11 must have identical character widths"
            ) {
                ArOsRow(
                    title = "Monospaced Clock A",
                    subtitle = "00:00:00",
                    trailingContent = {
                        Text(
                            text = "00:00:00",
                            style = ArOsTheme.typography.numericDisplay,
                            color = ArOsTheme.colors.accent
                        )
                    },
                    showDivider = true
                )
                ArOsRow(
                    title = "Monospaced Clock B",
                    subtitle = "11:11:11",
                    trailingContent = {
                        Text(
                            text = "11:11:11",
                            style = ArOsTheme.typography.numericDisplay,
                            color = ArOsTheme.colors.accent
                        )
                    },
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(ArOsTheme.spacing.sectionGap))

            // Section 2: Segmented Control
            ArOsGroup(header = "Segmented Control") {
                Column(modifier = Modifier.padding(ArOsTheme.spacing.md)) {
                    ArOsSegmentedControl(
                        options = listOf("Day", "Week", "Month", "Year"),
                        selectedIndex = selectedSegment,
                        onOptionSelected = { selectedSegment = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(ArOsTheme.spacing.sectionGap))

            // Section 3: Status Pills
            ArOsGroup(header = "Status Indicators") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ArOsTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArOsStatusPill(label = "Ready", tone = ArOsPillTone.Success)
                    ArOsStatusPill(label = "Active", tone = ArOsPillTone.Accent)
                    ArOsStatusPill(label = "Restricted", tone = ArOsPillTone.Warning)
                    ArOsStatusPill(label = "Critical", tone = ArOsPillTone.Danger)
                }
            }

            Spacer(modifier = Modifier.height(ArOsTheme.spacing.sectionGap))

            // Section 4: Buttons
            ArOsGroup(header = "Button Variants") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ArOsTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArOsButton(
                        text = "Primary Accent",
                        onClick = {},
                        variant = ArOsButtonVariant.Primary,
                        leadingIcon = Icons.Default.Bolt,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ArOsButton(
                        text = "Secondary Surface",
                        onClick = {},
                        variant = ArOsButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ArOsButton(
                        text = "Critical SOS Trigger",
                        onClick = {},
                        variant = ArOsButtonVariant.CriticalSos,
                        leadingIcon = Icons.Default.Security,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ArOsButton(
                        text = "Destructive Reset",
                        onClick = {},
                        variant = ArOsButtonVariant.Destructive,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(ArOsTheme.spacing.sectionGap))

            // Section 5: Inset Row Styling
            ArOsGroup(header = "Inset Groups & Rows", footer = "Standard iOS-style grouped settings container") {
                ArOsRow(
                    title = "Digital Guardian",
                    subtitle = "All apps restricted",
                    leadingIcon = Icons.Default.Security,
                    leadingBadgeColor = ArOsTheme.colors.accent,
                    showChevron = true,
                    onClick = {}
                )
                ArOsRow(
                    title = "Routine Engine",
                    subtitle = "Morning Protocol active",
                    leadingIcon = Icons.Default.Alarm,
                    leadingBadgeColor = ArOsTheme.colors.warning,
                    showChevron = true,
                    onClick = {}
                )
                ArOsRow(
                    title = "Adaptive Intelligence",
                    subtitle = "System load normal",
                    leadingIcon = Icons.Default.Info,
                    leadingBadgeColor = ArOsTheme.colors.success,
                    showChevron = true,
                    showDivider = false,
                    onClick = {}
                )
            }
        }
    }
}
