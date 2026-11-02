package com.ozyern.evolution.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.unveilIn
import androidx.compose.material3.ColorScheme
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.ozyern.evolution.ui.theme.light

typealias TransitionSpecReceiver<T> = AnimatedContentTransitionScope<Scene<T>>
typealias TransitionSpecFn = TransitionSpecReceiver<Route>.() -> ContentTransform
typealias PredictiveTransitionSpecFn = TransitionSpecReceiver<Route>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform

/** Approximates `UINavigationController`'s push/pop curve: a quick start with a long, soft landing */
private val NavigationEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val NavigationMillis = 400

/** How much of its width the screen underneath moves by, and how dimmed it gets */
private const val ParallaxDivisor = 3
private const val DimmedAlpha = 0.6f

/**
 * iOS' push: the new screen slides over from the trailing edge, while the current one drifts a little
 * towards the leading edge and dims. Should be paired with [iosPopTransitionSpec] for the pop transition.
 */
fun iosPushTransitionSpec(): TransitionSpecReceiver<*>.() -> ContentTransform = {
    ContentTransform(
        targetContentEnter = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(NavigationMillis, easing = NavigationEasing),
            initialOffset = { it },
        ),
        initialContentExit = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(NavigationMillis, easing = NavigationEasing),
            targetOffset = { it / ParallaxDivisor },
        ) + fadeOut(tween(NavigationMillis, easing = NavigationEasing), targetAlpha = DimmedAlpha),
        targetContentZIndex = 1f,
    )
}

/** The reverse of [iosPushTransitionSpec]: the top screen slides away, uncovering the previous one */
fun iosPopTransitionSpec(): TransitionSpecFn = {
    ContentTransform(
        targetContentEnter = slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(NavigationMillis, easing = NavigationEasing),
            initialOffset = { it / ParallaxDivisor },
        ) + fadeIn(tween(NavigationMillis, easing = NavigationEasing), initialAlpha = DimmedAlpha),
        initialContentExit = slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(NavigationMillis, easing = NavigationEasing),
            targetOffset = { it },
        ),
        targetContentZIndex = -1f,
    )
}

/** iOS follows the finger the same way regardless of edge, so the swipe's edge is ignored */
fun iosPredictivePopTransitionSpec(): PredictiveTransitionSpecFn = {
    iosPopTransitionSpec().invoke(this)
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

