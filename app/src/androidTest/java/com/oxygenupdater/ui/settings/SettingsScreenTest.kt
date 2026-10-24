package com.oxygenupdater.ui.settings

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.UsesSharedPreferencesTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.get
import com.oxygenupdater.internal.settings.KeyShareAnalyticsAndLogs
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.ui.common.IconTextButtonTestTag
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.BottomSheetTestTag
import com.oxygenupdater.ui.dialogs.BottomSheet_ItemRowTestTag
import com.oxygenupdater.ui.dialogs.Themes
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.utils.ContributorUtils
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest : UsesSharedPreferencesTest() {

    private val sheet = rule[BottomSheetTestTag]

    private lateinit var children: SemanticsNodeInteractionCollection

    /** "Support us" only has the contributor item, which is shown only on possibly-rooted devices */
    private val hasSupportSection = ContributorUtils.isAtLeastQAndPossiblyRooted

    /** Indices below are written as if the support section is shown */
    private fun child(index: Int) = children[if (hasSupportSection) index else index - 1]

    /**
     * Only required for Android 13/T+ to ensure notification item's summary is [R.string.summary_on]
     *
     * @see deviceSection
     */
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

        children = rule[SettingsScreenTestTag].run {
            assertHasScrollAction()
            onChildren()
        }.also { it.assertCountEquals(if (hasSupportSection) 17 else 16) }

        if (hasSupportSection) supportSection()
        deviceSection()
        uiSection()

        // By now we must have passed more than half the children, so
        // scroll down to the last child to keep remaining ones in view.
        children.onLast().performScrollTo()

        advancedSection()
        aboutSection()
    }

    private fun supportSection() {
        // This section has a surfaceContainer bg, so it'll be wrapped in `RectangleShape`.
        child(0).onChildren().run {
            this[0].assertHasTextExactly(R.string.preference_header_support)
        }
    }

    private fun deviceSection() {
        // Header
        child(1).assertHasTextExactly(R.string.preference_header_device)

        // DeviceChooser
        child(2).run {
            assertHasTextExactly(R.string.settings_device, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onDeviceSelect: ${DeviceSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // MethodChooser
        child(3).run {
            assertHasTextExactly(R.string.settings_update_method, androidx.compose.ui.R.string.not_selected)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onMethodSelect: ${MethodSettingsListConfig.list[0].id}")
            sheet.assertIsNotDisplayed()
        }

        // Notifications
        child(4).run {
            assertHasTextExactly(R.string.preference_header_notifications, R.string.summary_on)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }
    }

    private fun uiSection() {
        child(5).assertHasTextExactly(R.string.preference_header_ui)

        // Theme
        child(6).run {
            assertHasTextExactly(R.string.label_theme, Theme.System.titleResId)

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[0].assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onThemeSelect: ${Themes[0]}")
            sheet.assertIsNotDisplayed()
        }

        // Pure black
        child(7).assertHasTextExactly(R.string.settings_pure_black, R.string.summary_off)

        // Language
        child(8).run {
            assertHasTextExactly(
                R.string.label_language,
                with(currentLocale) {
                    displayName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(this) else it.toString()
                    }
                },
            )

            if (SDK_INT >= VERSION_CODES.TIRAMISU) {
                sheet.assertIsNotDisplayed() // should open system's app locale screen
                assertHasClickAction()
                // Don't performClick because that'll leave the app, which will fail subsequent tests
            } else {
                assertAndPerformClick()
                // Our own sheet is used only below Android 13
                sheet.assertIsDisplayed()
                rule.onAllNodesWithTag(BottomSheet_ItemRowTestTag)[1].assertAndPerformClick()
                sheet.assertIsNotDisplayed()
            }
        }
    }

    private fun advancedSection() {
        child(9).assertHasTextExactly(R.string.preference_header_advanced)

        // Advanced mode switch
        child(10).run {
            assertHasTextExactly(R.string.settings_advanced_mode, R.string.summary_off)
            val switch = onChild().assertIsOff()

            assertAndPerformClick()
            sheet.assertIsDisplayed()
            rule[IconTextButtonTestTag].assertAndPerformClick()
            sheet.assertIsNotDisplayed()
            ensureCallbackInvokedExactlyOnce("onAdvancedModeChange: true")
            assertHasTextExactly(R.string.settings_advanced_mode, R.string.summary_on)
            switch.assertIsOn()
        }

        // Analytics switch
        child(11).run {
            assertHasTextExactly(R.string.settings_upload_logs, R.string.summary_on)
            val switch = onChild().assertIsOn()

            assertAndPerformClick()
            sheet.assertIsNotDisplayed()
            ensureCallbackInvokedExactlyOnce("persistBool: $KeyShareAnalyticsAndLogs=false")
            assertHasTextExactly(R.string.settings_upload_logs, R.string.summary_off)
            switch.assertIsOff()
        }

        // Clear cache (flaky test, depends on cache size)
        // This is disabled if disk cache size is 0, which should be the case for tests.
        // Compose M3 wraps disabled elements in a `RectangleShape`.
        child(12).assertHasNoClickAction().onChildren().run {
            this[1].assertHasTextExactly(R.string.settings_clear_cache)
            this[2].assertHasTextExactly(activity.formatFileSize(0))
        }
    }

    private fun aboutSection() {
        child(13).assertHasTextExactly(R.string.preference_header_about)

        // Privacy policy
        child(14).run {
            assertHasTextExactly(R.string.label_privacy_policy, R.string.summary_privacy_policy)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }

        // Rate app
        child(15).run {
            assertHasTextExactly(R.string.label_rate_app, R.string.summary_rate_app)
            assertHasClickAction()
            // Don't performClick because that'll leave the app, which will fail subsequent tests
        }

        // App version
        child(16).run {
            assertHasTextExactly(R.string.app_name, "v${BuildConfig.VERSION_NAME}")
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("openAboutScreen")
        }
    }
}
