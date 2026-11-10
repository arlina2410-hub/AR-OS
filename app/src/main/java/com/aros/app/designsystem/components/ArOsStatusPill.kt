package com.aros.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.theme.ArOsTheme

enum class ArOsPillTone {
    Success,
    Warning,
    Danger,
    Accent,
    Neutral
}

@Composable
fun ArOsStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    tone: ArOsPillTone = ArOsPillTone.Neutral,
    showDot: Boolean = true
) {
    val (dotColor, bgTint, textColor) = when (tone) {
        ArOsPillTone.Success -> Triple(
            ArOsTheme.colors.success,
            ArOsTheme.colors.success.copy(alpha = 0.15f),
            ArOsTheme.colors.success
        )
        ArOsPillTone.Warning -> Triple(
            ArOsTheme.colors.warning,
            ArOsTheme.colors.warning.copy(alpha = 0.15f),
            ArOsTheme.colors.warning
        )
        ArOsPillTone.Danger -> Triple(
            ArOsTheme.colors.danger,
            ArOsTheme.colors.danger.copy(alpha = 0.15f),
            ArOsTheme.colors.danger
        )
        ArOsPillTone.Accent -> Triple(
            ArOsTheme.colors.accent,
            ArOsTheme.colors.accent.copy(alpha = 0.15f),
            ArOsTheme.colors.accent
        )
        ArOsPillTone.Neutral -> Triple(
            ArOsTheme.colors.contentTertiary,
            ArOsTheme.colors.surfaceRaised,
            ArOsTheme.colors.contentSecondary
        )
    }

    Row(
        modifier = modifier
            .clip(ArOsTheme.shapes.pill)
            .background(bgTint, ArOsTheme.shapes.pill)
            .border(0.8.dp, dotColor.copy(alpha = 0.35f), ArOsTheme.shapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(ArOsTheme.shapes.pill)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = ArOsTheme.typography.caption,
            color = textColor
        )
    }
}
