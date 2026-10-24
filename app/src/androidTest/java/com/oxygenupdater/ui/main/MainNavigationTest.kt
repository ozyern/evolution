package com.oxygenupdater.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.get
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Before
import org.junit.Test

class MainNavigationTest : ComposeBaseTest() {

    private val itemCount = MainRoutes.size
    private var currentRoute by mutableStateOf<MainRoute>(MainRoute.Update)

    @Before
    fun setup() {
        currentRoute = MainRoute.Update
    }

    @Test
    fun mainNavigation_bar() {
        setContent {
            MainLiquidDock(
                currentRoute = currentRoute,
                backdrop = rememberLayerBackdrop(),
                navigateTo = { currentRoute = it },
            )
        }

        // The dock is icon-only; labels are exposed as content descriptions
        rule[MainNavigation_BarTestTag].assertExists()
        rule.onAllNodes(IsTab).validateItems { route -> assertContentDescriptionEquals(activity.getString(route.labelResId)) }
    }

    @Test
    fun mainNavigation_verticalDock() {
        setContent {
            MainLiquidDock(
                currentRoute = currentRoute,
                backdrop = rememberLayerBackdrop(),
                navigateTo = { currentRoute = it },
                vertical = true,
            )
        }

        rule[MainNavigation_BarTestTag].assertExists()
        rule.onAllNodes(IsTab).validateItems { route -> assertContentDescriptionEquals(activity.getString(route.labelResId)) }
    }

    private fun SemanticsNodeInteractionCollection.validateItems(
        assertLabel: SemanticsNodeInteraction.(MainRoute) -> Unit,
    ) = repeat(itemCount) { index ->
        val route = MainRoutes[index]
        val child = get(index)
        child.assertLabel(route)

        if (route == currentRoute) child.assertIsSelected() else child.assertIsNotSelected()

        child.assertHasClickAction(); child.performClick(); advanceFrame() // should change currentRoute
        assert(route == currentRoute) {
            "Current route did not change. Expected: $route, actual: $currentRoute."
        }
    }

    private companion object {
        val IsTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    }
}
