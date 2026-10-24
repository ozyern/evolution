package com.oxygenupdater.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.unveilIn
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.oxygenupdater.ui.theme.light

typealias TransitionSpecReceiver<T> = AnimatedContentTransitionScope<Scene<T>>
typealias TransitionSpecFn = TransitionSpecReceiver<Route>.() -> ContentTransform
typealias PredictiveTransitionSpecFn = TransitionSpecReceiver<Route>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform

/**
 * Simple transition to convey the back stack to the user: when popping, both the outgoing and
 * incoming screens scale down while simultaneously fading in/out respectively.
 *
 * The outgoing screen scales down to 0.7x its original size, and the incoming screen scales down
 * from 1.1x its final size.
 *
 * Should be paired with [scaleChildTransitionSpec] for child routes.
 */
fun scalePopTransitionSpec(): TransitionSpecFn = {
    ContentTransform(
        targetContentEnter = scaleIn(initialScale = 1.1f) + fadeIn(),
        initialContentExit = scaleOut(targetScale = 0.7f) + fadeOut(),
    )
}

fun directionalScalePredictivePopTransitionSpec(): PredictiveTransitionSpecFn = { edge ->
    ContentTransform(
        targetContentEnter = scaleIn(initialScale = 1.1f) + fadeIn(),
        initialContentExit = scaleOut(
            targetScale = 0.7f, transformOrigin = when (edge) {
                NavigationEvent.EDGE_LEFT -> TransformOrigin(1f, 0.5f)
                NavigationEvent.EDGE_RIGHT -> TransformOrigin(0f, 0.5f)
                else -> TransformOrigin.Center
            }
        ) + fadeOut(),
    )
}

/**
 * Simple transition to convey the back stack to the user: when navigating, both the outgoing and
 * incoming screens scale up while simultaneously fading in/out respectively.
 *
 * The outgoing screen scales up to 1.1x its original size, and the incoming screen scales up
 * from 0.7x its final size.
 *
 * Should be paired with [scalePopTransitionSpec] for the pop transition.
 */
fun scaleChildTransitionSpec(): TransitionSpecReceiver<*>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = scaleIn(initialScale = 0.7f) + fadeIn(),
        initialContentExit = scaleOut(targetScale = 1.1f) + fadeOut(),
    )
}

/**
 * Kept in case I decide to change the [androidx.navigation3.ui.NavDisplay] transitions later.
 * This mimics the M3-recommended forward/backward transition: slide horizontally while fading
 * in/out respectively.
 */
fun slidePopTransitionSpec(): TransitionSpecFn = {
    // Half the duration of predictive pop
    val durationMillis = AnimationConstants.DefaultDurationMillis / 2
    ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearOutSlowInEasing,
            ),
            // Start off slightly off the screen
            initialOffsetX = { -it / 4 },
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutLinearInEasing, // match exit
            ),
        ),
        initialContentExit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutLinearInEasing,
            ),
            // Reduce visual load by moving the content only a little bit,
            // relying on fade out for the rest.
            targetOffsetX = { it / 4 },
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutLinearInEasing,
            ),
        ),
    )
}

/**
 * Kept in case I decide to change the [androidx.navigation3.ui.NavDisplay] transitions later.
 * This mimics the M3-recommended forward/backward transition: slide horizontally while fading
 * in/out respectively.
 */
fun scrimmedContainerSlidePredictivePopTransitionSpec(colorScheme: ColorScheme): PredictiveTransitionSpecFn = {
    // We always enter from the left and exit from the right, regardless of swipe edge.
    // This mimics Android's animation between 2 apps.
    @OptIn(ExperimentalAnimationApi::class)
    ContentTransform(
        targetContentEnter = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(easing = LinearOutSlowInEasing),
            // Start off slightly off the screen
            initialOffset = { -it / 4 },
        ) + unveilIn(
            // scrim overlay to indicate it's 'below' the exiting screen
            animationSpec = tween(easing = FastOutLinearInEasing), // match exit
            initialColor = colorScheme.run {
                // Use a dark veil on both themes. Surface colours are
                // preferred over Black (or scrim) to maintain theme colours.
                if (light) inverseSurface.copy(alpha = 0.7f) else surface
            },
        ),
        initialContentExit = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(easing = FastOutLinearInEasing),
            targetOffset = { it },
        ),
    )
}

