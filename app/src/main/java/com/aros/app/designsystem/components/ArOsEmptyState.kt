package com.aros.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.theme.ArOsTheme

@Composable
fun ArOsEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ArOsTheme.spacing.screenMargin,
                vertical = ArOsTheme.spacing.xxl
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(ArOsTheme.shapes.lg)
                .background(ArOsTheme.colors.surfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ArOsTheme.colors.contentSecondary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(ArOsTheme.spacing.md))

        Text(
            text = title,
            style = ArOsTheme.typography.headline,
            color = ArOsTheme.colors.contentPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ArOsTheme.spacing.xs))

        Text(
            text = description,
            style = ArOsTheme.typography.subhead,
            color = ArOsTheme.colors.contentSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = ArOsTheme.spacing.md)
        )

        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(ArOsTheme.spacing.lg))
            ArOsButton(
                text = actionButtonText,
                onClick = onActionClick,
                variant = ArOsButtonVariant.Secondary
            )
        }
    }
}
