package com.ozyern.evolution.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private const val SpokeCount = 12

/**
 * iOS' spinning-spokes activity indicator (`UIActivityIndicatorView`).
 *
 * @param revealed fraction of spokes to show. Anything below 1 draws a static, partially revealed
 *   indicator (as when pulling to refresh); 1 spins.
 */
@Composable
fun ActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    revealed: () -> Float = { 1f },
) {
    val head = rememberInfiniteTransition("ActivityIndicator").animateFloat(
        initialValue = 0f,
        targetValue = SpokeCount.toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "ActivityIndicatorHead",
    )

    Canvas(
        modifier
            .progressSemantics()
            .size(size)
    ) {
        val fraction = revealed().coerceIn(0f, 1f)
        val spinning = fraction >= 1f
        val visible = if (spinning) SpokeCount else (fraction * SpokeCount).toInt()
        val first = head.value.toInt()
        val strokeWidth = this.size.width * 0.09f
        val center = Offset(this.size.width / 2, this.size.height / 2)

        for (i in 0 until visible) {
            // The spoke at `first` is brightest, and trailing spokes fade out behind it
            val age = if (spinning) (first - i + SpokeCount) % SpokeCount else 0
            rotate(i * 360f / SpokeCount, center) {
                drawLine(
                    color = color.copy(alpha = 1f - age * 0.8f / SpokeCount),
                    start = Offset(center.x, this.size.height * 0.08f),
                    end = Offset(center.x, this.size.height * 0.29f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

/**
 * Thin, rounded progress bar, like `UIProgressView`.
 *
 * @param progress current value in 0..1, or `null` for an indeterminate bar
 */
@Composable
fun ProgressBar(
    progress: (() -> Float)?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val sweep = if (progress == null) rememberInfiniteTransition("ProgressBar").animateFloat(
        initialValue = -0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "ProgressBarSweep",
    ) else null

    Canvas(
        modifier
            .then(
                if (progress == null) Modifier.progressSemantics()
                else Modifier.progressSemantics(progress().coerceIn(0f, 1f))
            )
            .height(4.dp)
    ) {
        val width = this.size.width
        val height = this.size.height
        val radius = CornerRadius(height / 2)
        drawRoundRect(track, cornerRadius = radius)

        val start: Float
        val length: Float
        if (sweep != null) {
            start = sweep.value.coerceAtLeast(0f) * width
            length = ((sweep.value + 0.3f).coerceAtMost(1f) * width - start).coerceAtLeast(0f)
        } else {
            start = 0f
            length = progress!!().coerceIn(0f, 1f) * width
        }

        // Grow from the leading edge, which is on the right in RTL layouts
        val x = if (layoutDirection == LayoutDirection.Rtl) width - start - length else start
        drawRoundRect(color, topLeft = Offset(x, 0f), size = Size(length, height), cornerRadius = radius)
    }
}
