package com.oxygenupdater.ui.onboarding

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.oxygenupdater.R
import com.oxygenupdater.UsesSharedPreferencesTest
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.ui.dialogs.BottomSheetTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_ItemRowTestTag
import com.oxygenupdater.ui.settings.DeviceSettingsListConfig
import com.oxygenupdater.ui.settings.MethodSettingsListConfig
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class OnboardingScreenTest : UsesSharedPreferencesTest() {

    @Test
    fun onboardingScreen_expanded() = validate(WindowWidthSizeClass.Expanded)

    @Test
    fun onboardingScreen_compact() = validate(WindowWidthSizeClass.Compact)

    private fun validate(windowWidthSize: WindowWidthSizeClass) {
        setContent(windowWidthSize)

        rule[OnboardingScreen_MainColumnTestTag].assertHasScrollAction()
        rule.onNodeWithText(
            activity.getString(R.string.onboarding_welcome_title, activity.getString(R.string.app_name))
        ).assertIsDisplayed()

        val sheet = rule[BottomSheetTestTag]

        // DeviceChooser
        rule.onNodeWithText(activity.getString(R.string.settings_device)).run {
            performScrollTo()
            performClick()
        }
        sheet.assertIsDisplayed()
        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].performClick()
        ensureCallbackInvokedExactlyOnce("onDeviceSelect: ${DeviceSettingsListConfig.list[0].id}")
        sheet.assertIsNotDisplayed()

        // MethodChooser
        rule.onNodeWithText(activity.getString(R.string.settings_update_method)).run {
            performScrollTo()
            performClick()
        }
        sheet.assertIsDisplayed()
        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].performClick()
        ensureCallbackInvokedExactlyOnce("onMethodSelect: ${MethodSettingsListConfig.list[0].id}")
        sheet.assertIsNotDisplayed()

        rule[OnboardingScreen_DisclaimerTestTag].run {
            performScrollTo()
            assertHasTextExactly(R.string.onboarding_disclaimer)
        }

        // Continue button
        rule.onNodeWithText(activity.getString(R.string.onboarding_continue)).performClick()
        ensureCallbackInvokedExactlyOnce("onStartAppClick: true")
    }

    private fun setContent(windowWidthSize: WindowWidthSizeClass) = setContent {
        OnboardingScreen(
            windowWidthSize = windowWidthSize,
            scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            deviceConfig = DeviceSettingsListConfig,
            onDeviceSelect = { trackCallback("onDeviceSelect: ${it.id}") },
            methodConfig = MethodSettingsListConfig,
            onMethodSelect = { trackCallback("onMethodSelect: ${it.id}") },
            getPrefStr = ::getPrefStr,
            getPrefBool = ::getPrefBool,
            persistBool = ::persistBool,
            onStartAppClick = { trackCallback("onStartAppClick: $it") },
        )
    }
}
