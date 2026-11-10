package com.aros.app.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * AR/OS Shapes and Corner Radii tokens.
 */
@Immutable
data class ArOsShapes(
    val xs: Shape = RoundedCornerShape(8.dp),
    val sm: Shape = RoundedCornerShape(12.dp),
    val md: Shape = RoundedCornerShape(14.dp),
    val lg: Shape = RoundedCornerShape(20.dp),
    val xl: Shape = RoundedCornerShape(28.dp),
    val pill: Shape = CircleShape
)

val LocalArOsShapes = staticCompositionLocalOf { ArOsShapes() }
