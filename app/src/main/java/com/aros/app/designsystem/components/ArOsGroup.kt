package com.aros.app.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.theme.ArOsTheme

/**
 * Inset-grouped list container modeled after premium iOS settings & health sections.
 */
@Composable
fun ArOsGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ArOsTheme.spacing.xs)
    ) {
        if (!header.isNullOrBlank()) {
            Text(
                text = header.uppercase(),
                style = ArOsTheme.typography.caption,
                color = ArOsTheme.colors.contentTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = ArOsTheme.spacing.screenMargin,
                        vertical = ArOsTheme.spacing.xs
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ArOsTheme.spacing.screenMargin)
                .clip(ArOsTheme.shapes.lg)
                .background(ArOsTheme.colors.surface)
                .border(
                    BorderStroke(1.dp, ArOsTheme.colors.hairline),
                    ArOsTheme.shapes.lg
                )
        ) {
            content()
        }

        if (!footer.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(ArOsTheme.spacing.xxs))
            Text(
                text = footer,
                style = ArOsTheme.typography.footnote,
                color = ArOsTheme.colors.contentTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = ArOsTheme.spacing.screenMargin,
                        vertical = ArOsTheme.spacing.xxs
                    )
            )
        }
    }
}
