package com.aros.app.designsystem.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.aros.app.designsystem.motion.ArOsPressIndication

/**
 * Top-level design system access object.
 */
object ArOsTheme {
    val colors: ArOsColors
        @Composable
        @ReadOnlyComposable
        get() = LocalArOsColors.current

    val typography: ArOsTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalArOsTypography.current

    val spacing: ArOsSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalArOsSpacing.current

    val shapes: ArOsShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalArOsShapes.current
}

/**
 * Primary theme wrapper for AR/OS.
 */
@Composable
fun ArOsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val arOsColors = if (darkTheme) DarkArOsColors else LightArOsColors
    val typography = ArOsTypography()
    val spacing = ArOsSpacing()
    val shapes = ArOsShapes()

    val m3Colors = if (darkTheme) {
        darkColorScheme(
            primary = arOsColors.accent,
            onPrimary = arOsColors.contentOnAccent,
            primaryContainer = arOsColors.surfaceRaised,
            onPrimaryContainer = arOsColors.contentPrimary,
            secondary = arOsColors.accent,
            onSecondary = arOsColors.contentOnAccent,
            background = arOsColors.background,
            onBackground = arOsColors.contentPrimary,
            surface = arOsColors.surface,
            onSurface = arOsColors.contentPrimary,
            surfaceVariant = arOsColors.surfaceRaised,
            onSurfaceVariant = arOsColors.contentSecondary,
            outline = arOsColors.hairline,
            outlineVariant = arOsColors.separator,
            error = arOsColors.danger,
            onError = arOsColors.contentOnAccent
        )
    } else {
        lightColorScheme(
            primary = arOsColors.accent,
            onPrimary = arOsColors.contentOnAccent,
            primaryContainer = arOsColors.surfaceRaised,
            onPrimaryContainer = arOsColors.contentPrimary,
            secondary = arOsColors.accent,
            onSecondary = arOsColors.contentOnAccent,
            background = arOsColors.background,
            onBackground = arOsColors.contentPrimary,
            surface = arOsColors.surface,
            onSurface = arOsColors.contentPrimary,
            surfaceVariant = arOsColors.surfaceRaised,
            onSurfaceVariant = arOsColors.contentSecondary,
            outline = arOsColors.hairline,
            outlineVariant = arOsColors.separator,
            error = arOsColors.danger,
            onError = arOsColors.contentOnAccent
        )
    }

    CompositionLocalProvider(
        LocalArOsColors provides arOsColors,
        LocalArOsTypography provides typography,
        LocalArOsSpacing provides spacing,
        LocalArOsShapes provides shapes,
        LocalIndication provides ArOsPressIndication
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )
    }
}
