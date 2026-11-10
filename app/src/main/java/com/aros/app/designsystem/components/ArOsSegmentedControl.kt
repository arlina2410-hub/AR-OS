package com.aros.app.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aros.app.designsystem.haptics.rememberArOsHaptics
import com.aros.app.designsystem.motion.ArOsMotion
import com.aros.app.designsystem.theme.ArOsTheme

@Composable
fun ArOsSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    val haptics = rememberArOsHaptics()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(ArOsTheme.shapes.sm)
            .background(ArOsTheme.colors.surfaceRaised)
            .border(1.dp, ArOsTheme.colors.hairline, ArOsTheme.shapes.sm)
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = ArOsMotion.springSnappy,
            label = "segment_indicator"
        )

        // Sliding indicator pill
        Box(
            modifier = Modifier
                .width(segmentWidth)
                .fillMaxHeight()
                .offset(x = segmentWidth * indicatorOffset)
                .clip(ArOsTheme.shapes.xs)
                .background(ArOsTheme.colors.surface)
                .border(0.8.dp, ArOsTheme.colors.hairline, ArOsTheme.shapes.xs)
        )

        // Labels
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (index != selectedIndex) {
                                haptics.tick()
                                onOptionSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = ArOsTheme.typography.footnote.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) {
                            ArOsTheme.colors.contentPrimary
                        } else {
                            ArOsTheme.colors.contentSecondary
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
