package com.ozyern.evolution.ui.common.liquid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.ozyern.evolution.ui.theme.light
import kotlinx.coroutines.flow.collectLatest

/**
 * iOS-style switch whose thumb turns into a Liquid Glass lens while pressed/dragged.
 * Tap to toggle, or drag the thumb.
 *
 * Ported from [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) (Apache-2.0).
 *
 * @param backdrop what's behind the switch, e.g. a [com.kyant.backdrop.backdrops.rememberCanvasBackdrop]
 *   that draws the card colour
 */
@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = colorScheme.primary
    val trackColor = if (colorScheme.light) Color(0xFF787878).copy(0.2f) else Color(0xFF787880).copy(0.36f)

    val latestChecked by rememberUpdatedState(checked)
    val latestOnCheckedChange by rememberUpdatedState(onCheckedChange)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStopped = {
                fraction = if (didDrag) {
                    didDrag = false
                    if (targetValue >= 0.5f) 1f else 0f
                } else if (latestChecked) 0f else 1f
                latestOnCheckedChange(fraction == 1f)
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) didDrag = dragAmount.x != 0f
                val delta = dragAmount.x / dragWidth
                fraction = (if (isLtr) fraction + delta else fraction - delta).fastCoerceIn(0f, 1f)
            },
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }.collectLatest { dampedDragAnimation.updateValue(it) }
    }
    // External changes (e.g. confirmation sheets) => move the thumb
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (target != fraction) {
            fraction = target
            dampedDragAnimation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier.semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind { drawRect(lerp(trackColor, accentColor, dampedDragAnimation.value)) }
                .size(64.dp, 28.dp)
        )

        Box(
            Modifier
                .graphicsLayer {
                    val padding = 2.dp.toPx()
                    translationX = if (isLtr) lerp(padding, padding + dragWidth, dampedDragAnimation.value)
                    else lerp(-padding, -(padding + dragWidth), dampedDragAnimation.value)
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            scale(lerp(2f / 3f, 0.75f, progress), lerp(0f, 0.75f, progress)) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8.dp.toPx() * (1f - progress))
                        lens(5.dp.toPx() * progress, 10.dp.toPx() * progress, chromaticAberration = true)
                    },
                    highlight = {
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = dampedDragAnimation.pressProgress,
                        )
                    },
                    shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.05f)) },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 4.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX = dampedDragAnimation.scaleX / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                        scaleY = dampedDragAnimation.scaleY * (1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f))
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - dampedDragAnimation.pressProgress)) },
                )
                .size(40.dp, 24.dp)
        )
    }
}
