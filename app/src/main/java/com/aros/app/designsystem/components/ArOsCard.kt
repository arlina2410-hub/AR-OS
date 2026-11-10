package com.aros.app.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.theme.ArOsTheme

/**
 * Standard raised surface card with 1dp hairline border and rounded corners.
 */
@Composable
fun ArOsCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ArOsTheme.colors.surface,
    borderColor: Color = ArOsTheme.colors.hairline,
    shape: Shape = ArOsTheme.shapes.lg,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor, shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(clickableModifier)
            .padding(ArOsTheme.spacing.md)
    ) {
        content()
    }
}
