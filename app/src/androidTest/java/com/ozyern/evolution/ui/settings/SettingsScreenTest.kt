package com.ozyern.evolution.ui.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ozyern.evolution.BuildConfig
import com.ozyern.evolution.R
import com.ozyern.evolution.UsesSharedPreferencesTest
import com.ozyern.evolution.assertHasScrollAction
import com.ozyern.evolution.get
import com.ozyern.evolution.ui.common.IconTextButtonTestTag
import com.ozyern.evolution.ui.dialogs.BottomSheetTestTag
import com.ozyern.evolution.ui.dialogs.BottomSheet_ItemRowTestTag
import com.ozyern.evolution.ui.dialogs.Themes
import com.ozyern.evolution.ui.main.NavType
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest : UsesSharedPreferencesTest() {

    private val sheet = rule[BottomSheetTestTag]

    /** Only required for Android 13/T+, so that notification status is consistent */
    @get:Rule
    val postNotificationsPermissionRule = GrantPostNotificationsPermissionRule()

    @Test
    fun settingsScreen() {
        setContent {
            SettingsScreen(
                navType = NavType.BottomBar,
                onContributorEnrollmentChange = { trackCallback("onContributorEnrollmentChange: $it") },
                deviceConfig = DeviceSettingsListConfig,
                onDeviceSelect = { trackCallback("onDeviceSelect: ${it.id}") },
                methodConfig = MethodSettingsListConfig,
                onMethodSelect = { trackCallback("onMethodSelect: ${it.id}") },
                onThemeSelect = { trackCallback("onThemeSelect: $it") },
                pureBlack = false,
                onPureBlackChange = { trackCallback("onPureBlackChange: $it") },
                onAdvancedModeChange = { trackCallback("onAdvancedModeChange: $it") },
                openAboutScreen = { trackCallback("openAboutScreen") },
                getPrefStr = ::getPrefStr,
                getPrefBool = ::getPrefBool,
                persistBool = ::persistBool,
            )
        }

        rule[SettingsScreenTestTag].assertHasScrollAction()

        // Section headers
        listOf(
            R.string.preference_header_device,
            R.string.preference_header_ui,
            R.string.preference_header_advanced,
            R.string.preference_header_about,
        ).forEach { node(it).performScrollTo().assertIsDisplayed() }

        // Device chooser
        node(R.string.settings_device).performScrollTo().performClick()
        sheet.assertIsDisplayed()
        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].performClick()
        ensureCallbackInvokedExactlyOnce("onDeviceSelect: ${DeviceSettingsListConfig.list[0].id}")
        sheet.assertIsNotDisplayed()

        // Method chooser
        node(R.string.settings_update_method).performScrollTo().performClick()
        sheet.assertIsDisplayed()
        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].performClick()
        ensureCallbackInvokedExactlyOnce("onMethodSelect: ${MethodSettingsListConfig.list[0].id}")
        sheet.assertIsNotDisplayed()

        // Theme
        node(R.string.label_theme).performScrollTo().performClick()
        sheet.assertIsDisplayed()
        rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].performClick()
        ensureCallbackInvokedExactlyOnce("onThemeSelect: ${Themes[0]}")
        sheet.assertIsNotDisplayed()

        // Liquid Glass switches: pure black (off), then advanced mode (asks for confirmation first)
        val switches = rule.onAllNodes(IsSwitch)
        switches[0].assertIsOff()
        node(R.string.settings_pure_black).performScrollTo().performClick()
        ensureCallbackInvokedExactlyOnce("onPureBlackChange: true")

        switches[1].assertIsOff()
        node(R.string.settings_advanced_mode).performScrollTo().performClick()
        sheet.assertIsDisplayed()
        rule[IconTextButtonTestTag].performClick()
        sheet.assertIsNotDisplayed()
        ensureCallbackInvokedExactlyOnce("onAdvancedModeChange: true")
        switches[1].assertIsOn()

        // About
        node(R.string.label_privacy_policy).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("v${BuildConfig.VERSION_NAME}").performScrollTo().performClick()
        ensureCallbackInvokedExactlyOnce("openAboutScreen")
        node(R.string.settings_footer_free).performScrollTo().assertIsDisplayed()
    }

    private fun node(resId: Int) = rule.onNodeWithText(activity.getString(resId))

    private companion object {
        val IsSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
    }
}
