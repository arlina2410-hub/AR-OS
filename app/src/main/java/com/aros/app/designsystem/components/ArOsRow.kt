package com.aros.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.theme.ArOsTheme

/**
 * Standard inset row item for use inside [ArOsGroup].
 */
@Composable
fun ArOsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingBadgeColor: Color? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ArOsTheme.spacing.rowMinHeight)
                .then(clickableModifier)
                .padding(
                    horizontal = ArOsTheme.spacing.md,
                    vertical = ArOsTheme.spacing.sm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                if (leadingBadgeColor != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(ArOsTheme.shapes.xs)
                            .background(leadingBadgeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = ArOsTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(ArOsTheme.spacing.md))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = ArOsTheme.spacing.xs)
            ) {
                Text(
                    text = title,
                    style = ArOsTheme.typography.body,
                    color = ArOsTheme.colors.contentPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = ArOsTheme.typography.subhead,
                        color = ArOsTheme.colors.contentSecondary
                    )
                }
            }

            if (trailingContent != null) {
                trailingContent()
            }

            if (showChevron) {
                Spacer(modifier = Modifier.width(ArOsTheme.spacing.xs))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ArOsTheme.colors.contentTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) {
            val startInset = if (leadingIcon != null) 56.dp else ArOsTheme.spacing.md
            HorizontalDivider(
                modifier = Modifier.padding(start = startInset),
                thickness = 0.8.dp,
                color = ArOsTheme.colors.separator
            )
        }
    }
}
