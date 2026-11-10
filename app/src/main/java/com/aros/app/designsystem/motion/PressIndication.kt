package com.aros.app.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AR/OS tactile press indication.
 * Applies a subtle 0.97 scale transformation on press, returning with a spring on release.
 */
object ArOsPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return PressScaleNode(interactionSource)
    }

    override fun hashCode(): Int = "ArOsPressIndication".hashCode()
    override fun equals(other: Any?): Boolean = other === this
}

private class PressScaleNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private val scaleAnim = Animatable(1f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        scaleAnim.animateTo(
                            targetValue = 0.97f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        )
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        scaleAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioNoBouncy
                            )
                        )
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = scaleAnim.value
        if (s == 1f) {
            drawContent()
        } else {
            scale(s, center) {
                this@draw.drawContent()
            }
        }
    }
}
