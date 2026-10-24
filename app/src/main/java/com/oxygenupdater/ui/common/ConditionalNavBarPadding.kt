package com.oxygenupdater.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.oxygenupdater.ui.main.LiquidDockReservedHeight
import com.oxygenupdater.ui.main.NavType

/**
 * Content is always drawn edge-to-edge, behind the system navigation bar. If [navType] is
 * [NavType.BottomBar], the floating [com.oxygenupdater.ui.main.MainLiquidDock] is also drawn over
 * content, so we leave room for it as well. Otherwise, only the system bar inset is needed.
 *
 * Applying padding in the topmost parent composable instead would lose edge-to-edge immersiveness,
 * and the glass dock would have nothing to refract.
 *
 * Meant to be used as the last (vertical) composable in children of [com.oxygenupdater.activities.MainActivity]'s NavHost.
 */
@Composable
inline fun ConditionalNavBarPadding(navType: NavType) = Spacer(
    if (navType == NavType.BottomBar) Modifier
        .navigationBarsPadding()
        .height(LiquidDockReservedHeight)
    else Modifier.navigationBarsPadding()
)
