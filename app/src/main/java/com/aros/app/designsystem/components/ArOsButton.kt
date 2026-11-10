package com.aros.app.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.haptics.rememberArOsHaptics
import com.aros.app.designsystem.theme.ArOsTheme

enum class ArOsButtonVariant {
    Primary,
    Secondary,
    Destructive,
    Ghost,
    CriticalSos
}

@Composable
fun ArOsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ArOsButtonVariant = ArOsButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = ArOsTheme.shapes.md
) {
    val haptics = rememberArOsHaptics()
    val interactionSource = remember { MutableInteractionSource() }

    val (bg, contentColor, borderStroke) = when (variant) {
        ArOsButtonVariant.Primary -> {
            Triple(
                if (enabled) ArOsTheme.colors.accent else ArOsTheme.colors.accent.copy(alpha = 0.4f),
                ArOsTheme.colors.contentOnAccent,
                null
            )
        }
        ArOsButtonVariant.Secondary -> {
            Triple(
                if (enabled) ArOsTheme.colors.surfaceRaised else ArOsTheme.colors.surfaceRaised.copy(alpha = 0.4f),
                if (enabled) ArOsTheme.colors.contentPrimary else ArOsTheme.colors.contentTertiary,
                BorderStroke(1.dp, ArOsTheme.colors.hairline)
            )
        }
        ArOsButtonVariant.Destructive -> {
            Triple(
                if (enabled) ArOsTheme.colors.danger else ArOsTheme.colors.danger.copy(alpha = 0.4f),
                ArOsTheme.colors.contentOnAccent,
                null
            )
        }
        ArOsButtonVariant.Ghost -> {
            Triple(
                Color.Transparent,
                if (enabled) ArOsTheme.colors.accent else ArOsTheme.colors.contentTertiary,
                null
            )
        }
        ArOsButtonVariant.CriticalSos -> {
            Triple(
                if (enabled) ArOsTheme.colors.sosCritical else ArOsTheme.colors.sosCritical.copy(alpha = 0.4f),
                ArOsTheme.colors.contentOnAccent,
                null
            )
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = ArOsTheme.spacing.minTouchTarget)
            .clip(shape)
            .background(bg, shape)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) {
                if (variant == ArOsButtonVariant.CriticalSos || variant == ArOsButtonVariant.Destructive) {
                    haptics.heavy()
                } else {
                    haptics.click()
                }
                onClick()
            }
            .padding(
                horizontal = ArOsTheme.spacing.lg,
                vertical = ArOsTheme.spacing.sm
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(ArOsTheme.spacing.xs))
            }
            Text(
                text = text,
                style = ArOsTheme.typography.callout,
                color = contentColor
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(ArOsTheme.spacing.xs))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ArOsIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ArOsTheme.colors.contentPrimary,
    backgroundColor: Color = Color.Transparent,
    enabled: Boolean = true
) {
    val haptics = rememberArOsHaptics()
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(ArOsTheme.spacing.minTouchTarget)
            .clip(ArOsTheme.shapes.pill)
            .background(backgroundColor, ArOsTheme.shapes.pill)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) {
                haptics.click()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
    }
}
