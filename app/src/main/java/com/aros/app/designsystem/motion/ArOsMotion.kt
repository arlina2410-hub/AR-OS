package com.aros.app.designsystem.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * AR/OS Motion specifications.
 * Built around organic springs, subtle damping, and snappy tactile responses.
 */
object ArOsMotion {
    /**
     * Standard spring for general UI transitions (sheets, drawer, expands).
     */
    val springStandard = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Snappy spring for quick responsive interactions (toggles, segment switches).
     */
    val springSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Tactile bounce spring for press releases and button impacts.
     */
    val springRelease = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Motion offset spring for navigation slide transitions.
     */
    val offsetSpring: AnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )

    /**
     * Smooth alpha fade transitions.
     */
    val fadeInSpec = tween<Float>(durationMillis = 180)
    val fadeOutSpec = tween<Float>(durationMillis = 140)
}
