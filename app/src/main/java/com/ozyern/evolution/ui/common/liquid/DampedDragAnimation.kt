package com.ozyern.evolution.ui.common.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Springy value + press/scale animations that drive the dock's glass indicator.
 * All values are backed by [Animatable]s, so they can be read inside draw/layer blocks
 * without triggering recomposition.
 *
 * Ported from [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (Apache-2.0).
 */
@Stable
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    val valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    private val initialScale: Float,
    private val pressedScale: Float,
    private val onDragStopped: DampedDragAnimation.() -> Unit,
    private val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value get() = valueAnimation.value
    val targetValue get() = valueAnimation.targetValue
    val pressProgress get() = pressProgressAnimation.value
    val scaleX get() = scaleXAnimation.value
    val scaleY get() = scaleYAnimation.value
    val velocity get() = velocityAnimation.value

    val modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { press() },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            },
        ) { _, dragAmount -> onDrag(size, dragAmount) }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }.first {
                    abs(it - valueAnimation.targetValue) < threshold
                }
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(System.currentTimeMillis(), Offset(value, 0f))
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
