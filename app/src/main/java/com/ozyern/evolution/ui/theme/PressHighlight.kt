package com.ozyern.evolution.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * iOS-style touch feedback, replacing Material's ripple: a flat grey highlight that appears as soon as a
 * row is pressed, and fades out when released.
 */
object PressHighlight : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode = Node(interactionSource)

    override fun equals(other: Any?) = other === this
    override fun hashCode() = System.identityHashCode(this)

    private class Node(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
        private val alpha = Animatable(0f)

        override fun onAttach() {
            coroutineScope.launch {
                source.interactions.collect {
                    when (it) {
                        is PressInteraction.Press -> launch { alpha.snapTo(1f) }
                        is PressInteraction.Release, is PressInteraction.Cancel -> launch {
                            alpha.animateTo(0f, tween(FadeOutMillis))
                        }
                    }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (alpha.value > 0f) drawRect(HighlightColor, alpha = alpha.value)
        }
    }

    private const val FadeOutMillis = 250

    /** iOS' `systemGray`, translucent so it reads on both light & dark surfaces */
    private val HighlightColor = Color(0x338E8E93)
}
