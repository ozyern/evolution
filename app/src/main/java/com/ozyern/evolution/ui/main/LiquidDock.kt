package com.ozyern.evolution.ui.main

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.ozyern.evolution.ui.common.liquid.DampedDragAnimation
import com.ozyern.evolution.ui.common.liquid.InteractiveHighlight
import com.ozyern.evolution.ui.theme.light
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Thickness of the floating dock itself (height when horizontal, width when vertical), excluding margins */
private val DockThickness = 64.dp
private val DockMargin = 12.dp

/** Size of the lens along the dock's cross axis */
private val LensThickness = 56.dp

/** Size of each tab along the dock's main axis, used only for the vertical dock */
private val VerticalTabLength = 56.dp

/**
 * Space that content must leave next to the floating dock: at the bottom for the horizontal dock
 * (excluding the system navigation bar inset), or at the end for the vertical dock.
 *
 * @see com.ozyern.evolution.ui.common.ConditionalNavBarPadding
 */
val LiquidDockReservedHeight = DockThickness + DockMargin * 2

/** Blur isn't available below Android 12, so the dock must be more opaque to stay legible */
private val ContainerAlpha = if (SDK_INT >= VERSION_CODES.S) 0.4f else 0.88f

private val LocalLiquidTabScale = staticCompositionLocalOf { { 1f } }

/** Per-tab size modifier, provided by [LiquidTabs] based on orientation */
private val LocalLiquidTabSize = staticCompositionLocalOf<Modifier> { Modifier }

/**
 * Floating Liquid Glass dock for [MainRoutes], built on [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass).
 *
 * The glass refracts whatever's drawn into [backdrop] (i.e. the screen content beneath it). The selected
 * tab is marked by a draggable glass "lens", which magnifies and tints the icon under it. Users can tap
 * a tab, or press & drag the lens across tabs.
 *
 * @param vertical `true` for a vertical pill on the end edge (foldables/tablets, i.e. [NavType.SideRail]),
 *   `false` for a horizontal pill at the bottom (phones, i.e. [NavType.BottomBar]).
 */
@Composable
fun MainLiquidDock(
    currentRoute: Route?,
    backdrop: Backdrop,
    navigateTo: (route: MainRoute) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) = Box(
    contentAlignment = if (vertical) Alignment.CenterEnd else Alignment.BottomCenter,
    modifier = if (vertical) modifier
        .fillMaxHeight()
        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.End + WindowInsetsSides.Bottom))
        .padding(DockMargin)
    else modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp, vertical = DockMargin)
) {
    // Keep the last known main route selected while a child route is on top (e.g. during exit animation)
    var lastMainIndex by remember { mutableIntStateOf(0) }
    val routeIndex = MainRoutes.indexOf(currentRoute)
    val selectedIndex = if (routeIndex >= 0) routeIndex else lastMainIndex
    SideEffect { lastMainIndex = selectedIndex }
    // Must be read via State so that LiquidTabs' snapshotFlow observes changes
    val selectedIndexState = rememberUpdatedState(selectedIndex)

    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestNavigateTo by rememberUpdatedState(navigateTo)
    val onTabSelected: (Int) -> Unit = remember {
        { index ->
            val route = MainRoutes[index]
            // Avoid redundant back stack operations if we're already there
            if (route !== latestCurrentRoute) latestNavigateTo(route)
        }
    }

    val tabsCount = MainRoutes.size
    LiquidTabs(
        selectedTabIndex = { selectedIndexState.value },
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = tabsCount,
        vertical = vertical,
        modifier = (if (vertical) Modifier
            .width(DockThickness)
            .height(VerticalTabLength * tabsCount + 8.dp)
        else Modifier.widthIn(max = 480.dp)).testTag(MainNavigation_BarTestTag)
    ) {
        MainRoutes.forEachIndexed { index, screen ->
            val labelResId = screen.labelResId
            key(labelResId) {
                val label = stringResource(labelResId)
                LiquidTab(
                    selected = index == selectedIndex,
                    label = label,
                    onClick = { onTabSelected(index) },
                ) {
                    val badge = screen.badge
                    if (badge == null) Icon(screen.icon, null) else BadgedBox({
                        Badge { Text(badge.take(3)) }
                    }) { Icon(screen.icon, null) }
                }
            }
        }
    }
}

@Composable
private fun LiquidTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    vertical: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = BoxWithConstraints(modifier, contentAlignment = if (vertical) Alignment.TopCenter else Alignment.CenterStart) {
    val colorScheme = MaterialTheme.colorScheme
    val light = colorScheme.light
    val accentColor = colorScheme.primary
    val containerColor = colorScheme.surfaceContainer.copy(alpha = ContainerAlpha)
    val indicatorColor = if (light) Color.Black.copy(0.1f) else Color.White.copy(0.1f)

    val tabsBackdrop = rememberLayerBackdrop()

    val density = LocalDensity.current
    // Main axis: the one tabs are laid out along
    val mainAxisPx = (if (vertical) constraints.maxHeight else constraints.maxWidth).toFloat()
    val tabSizePx = with(density) { (mainAxisPx - 8.dp.toPx()) / tabsCount }
    val tabSize: Dp = with(density) { tabSizePx.toDp() }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density, mainAxisPx) {
        derivedStateOf {
            val fraction = (offsetAnimation.value / mainAxisPx).fastCoerceIn(-1f, 1f)
            with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
        }
    }

    // RTL only flips the horizontal dock; vertical always runs top to bottom
    val forward = vertical || LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(selectedTabIndex()) }
    val dampedDragAnimation = remember(animationScope, tabsCount, tabSizePx, vertical) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = currentIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, dragAmount ->
                val delta = if (vertical) dragAmount.y else dragAmount.x
                updateValue(
                    (targetValue + delta / tabSizePx * if (forward) 1f else -1f)
                        .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                )
                animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + delta) }
            },
        )
    }

    // External selection changes (taps, back presses, deep links) => move the lens
    LaunchedEffect(Unit) {
        snapshotFlow { selectedTabIndex() }.collect { currentIndex = it }
    }
    // Lens movement (from either source) => animate & notify
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }.drop(1).collect { index ->
            dampedDragAnimation.animateToValue(index.toFloat())
            onTabSelected(index)
        }
    }

    /** Lens centre along the main axis, in px, relative to the panel */
    fun lensCentre() = (dampedDragAnimation.value + 0.5f) * tabSizePx

    val interactiveHighlight = remember(animationScope, tabSizePx, vertical) {
        InteractiveHighlight(animationScope) { size, _ ->
            if (vertical) Offset(size.width / 2f, lensCentre() + panelOffset)
            else Offset(
                if (forward) lensCentre() + panelOffset
                else size.width - lensCentre() + panelOffset,
                size.height / 2f
            )
        }
    }

    val translatePanel: Modifier = Modifier.graphicsLayer {
        if (vertical) translationY = panelOffset else translationX = panelOffset
    }
    val panelSize = if (vertical) Modifier
        .width(DockThickness)
        .fillMaxHeight()
        .padding(4.dp) else Modifier
        .height(DockThickness)
        .fillMaxWidth()
        .padding(4.dp)
    val tabSizeModifier = if (vertical) Modifier
        .fillMaxWidth()
        .height(tabSize) else Modifier
        .fillMaxHeight()
        .width(tabSize)

    @Composable
    fun Panel(modifier: Modifier) = if (vertical) Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() } else Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }

    CompositionLocalProvider(LocalLiquidTabSize provides tabSizeModifier) {
        // Visible glass panel
        Panel(
            translatePanel
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(8.dp.toPx())
                        lens(24.dp.toPx(), 24.dp.toPx())
                    },
                    layerBlock = {
                        val extent = if (vertical) size.height else size.width
                        val scale = lerp(1f, 1f + 16.dp.toPx() / extent, dampedDragAnimation.pressProgress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) },
                )
                .then(interactiveHighlight.modifier)
                .then(panelSize)
                .selectableGroup()
        )

        // Invisible accent-tinted copy, recorded into `tabsBackdrop` so the lens can refract it.
        // This is what makes the icon under the lens appear in the accent colour.
        CompositionLocalProvider(
            LocalLiquidTabScale provides { lerp(1f, 1.2f, dampedDragAnimation.pressProgress) }
        ) {
            Panel(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .then(translatePanel)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(24.dp.toPx() * progress, 24.dp.toPx() * progress)
                        },
                        highlight = { Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress) },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .then(interactiveHighlight.modifier)
                    .then(panelSize)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor))
            )
        }
    }

    // The glass lens (selection indicator)
    Box(
        Modifier
            // Inset by the panel's inner padding along the main axis
            .padding(start = if (vertical) 0.dp else 4.dp, top = if (vertical) 4.dp else 0.dp)
            .graphicsLayer {
                val value = dampedDragAnimation.value
                if (vertical) translationY = value * tabSizePx + panelOffset
                else translationX = if (forward) value * tabSizePx + panelOffset
                else size.width - (value + 1f) * tabSizePx + panelOffset
            }
            .then(interactiveHighlight.gestureModifier)
            .then(dampedDragAnimation.modifier)
            .drawBackdrop(
                backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                shape = { Capsule() },
                effects = {
                    val progress = dampedDragAnimation.pressProgress
                    lens(10.dp.toPx() * progress, 14.dp.toPx() * progress, chromaticAberration = true)
                },
                highlight = { Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress) },
                shadow = { Shadow(alpha = dampedDragAnimation.pressProgress) },
                innerShadow = {
                    val progress = dampedDragAnimation.pressProgress
                    InnerShadow(radius = 8.dp * progress, alpha = progress)
                },
                layerBlock = {
                    // Stretch along the direction of motion, squash across it
                    val velocity = dampedDragAnimation.velocity / 10f
                    val stretch = 1f / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                    val squash = 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    scaleX = dampedDragAnimation.scaleX * if (vertical) squash else stretch
                    scaleY = dampedDragAnimation.scaleY * if (vertical) stretch else squash
                },
                onDrawSurface = {
                    val progress = dampedDragAnimation.pressProgress
                    drawRect(indicatorColor, alpha = 1f - progress)
                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                },
            )
            .then(
                if (vertical) Modifier.size(LensThickness, tabSize)
                else Modifier
                    .height(LensThickness)
                    .width(tabSize)
            )
    )
}

@Composable
private fun LiquidTab(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scale = LocalLiquidTabScale.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = LocalLiquidTabSize.current
            .clip(Capsule())
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = label }
            .graphicsLayer {
                val scale = scale()
                scaleX = scale
                scaleY = scale
            },
    ) { content() }
}

@VisibleForTesting
const val MainNavigation_BarTestTag = "MainNavigation_Bar"
