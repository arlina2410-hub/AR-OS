package com.aros.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.haptics.rememberArOsHaptics
import com.aros.app.designsystem.theme.ArOsTheme

enum class ArOsNavDestination(
    val label: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Default.Home),
    Safety("Safety", Icons.Default.HealthAndSafety),
    Rhythms("Rhythms", Icons.Default.Schedule),
    Focus("Focus", Icons.Default.CenterFocusStrong),
    More("More", Icons.Default.MoreHoriz)
}

@Composable
fun ArOsBottomBar(
    currentDestination: ArOsNavDestination,
    onNavigate: (ArOsNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberArOsHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ArOsTheme.colors.surfaceRaised.copy(alpha = 0.95f))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 0.8.dp,
            color = ArOsTheme.colors.hairline
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = ArOsTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArOsNavDestination.entries.forEach { destination ->
                val isSelected = destination == currentDestination
                val iconTint = if (isSelected) {
                    if (destination == ArOsNavDestination.Safety) ArOsTheme.colors.danger else ArOsTheme.colors.accent
                } else {
                    ArOsTheme.colors.contentTertiary
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(ArOsTheme.shapes.sm)
                        .clickable(
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptics.click()
                                onNavigate(destination)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = destination.label,
                        style = ArOsTheme.typography.caption,
                        color = iconTint,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
