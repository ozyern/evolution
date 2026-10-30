package com.ozyern.evolution.ui.update

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ozyern.evolution.R
import com.ozyern.evolution.UsesSharedPreferencesTest
import com.ozyern.evolution.assertAndPerformClick
import com.ozyern.evolution.get
import com.ozyern.evolution.internal.NotSet
import com.ozyern.evolution.internal.settings.KeyAdvancedMode
import com.ozyern.evolution.models.UpdateData
import com.ozyern.evolution.ui.RefreshAwareState
import com.ozyern.evolution.ui.common.ErrorStateTestTag
import com.ozyern.evolution.ui.common.ErrorState_TitleTestTag
import com.ozyern.evolution.ui.common.IconTextButtonTestTag
import com.ozyern.evolution.ui.common.RichText_ContainerTestTag
import com.ozyern.evolution.ui.main.MainRoute
import com.ozyern.evolution.ui.main.NavType
import com.ozyern.evolution.ui.theme.PreviewWindowSize
import org.junit.Test

class UpdateScreenTest : UsesSharedPreferencesTest() {

    private var data by mutableStateOf<UpdateData?>(null)
    private var currentGetPrefBool by mutableStateOf<(String, Boolean) -> Boolean>(::getPrefBool)

    @Test
    fun updateScreen_nullData() {
        data = null; setContent()

        rule[ErrorState_TitleTestTag].assertHasTextExactly(R.string.update_information_error_title)
        rule[RichText_ContainerTestTag].assertExists()

        rule[IconTextButtonTestTag].assertAndPerformClick()
        ensureCallbackInvokedExactlyOnce("onRefresh")

        rule[UpToDateContentTestTag].assertDoesNotExist()
        rule[UpdateAvailableContentTestTag].assertDoesNotExist()
    }

    @Test
    fun updateScreen_upToDate() {
        /** Condition 1: [UpdateData.id] == null */
        data = UpdateData(
            id = null,
            versionNumber = "versionNumber",
            updateInformationAvailable = true,
            systemIsUpToDate = false,
        ); setContent(true)
        common_upToDate(R.string.update_information_system_is_up_to_date)

        /** Condition 2: [UpdateData.isUpdateInformationAvailable] == false */
        data = UpdateData(
            id = 1,
            versionNumber = null,
            updateInformationAvailable = false,
            systemIsUpToDate = false,
        )
        common_upToDate(R.string.update_information_no_update_data_available)

        /** Condition 3: [UpdateData.systemIsUpToDate] == true & advanced mode is disabled */
        data = UpdateData(
            id = 1,
            versionNumber = "versionNumber",
            updateInformationAvailable = true,
            systemIsUpToDate = true,
        )
        currentGetPrefBool = { key, default -> if (key == KeyAdvancedMode) false else default }
        common_upToDate(R.string.update_information_system_is_up_to_date)
    }

    @Test
    fun updateScreen_updateAvailable() {
        /** [UpdateData.systemIsUpToDate] == false */
        data = UpdateData(
            id = 1,
            versionNumber = "versionNumber",
            updateInformationAvailable = true,
            systemIsUpToDate = false,
        ); setContent(true)
        advanceFrame(); assertBadge("new")
        common_updateAvailable(R.string.update_notification_channel_name)

        /** [UpdateData.systemIsUpToDate] == true & advanced mode is enabled */
        data = UpdateData(
            id = 1,
            versionNumber = "versionNumber",
            updateInformationAvailable = true,
            systemIsUpToDate = true,
        )
        currentGetPrefBool = { key, default -> if (key == KeyAdvancedMode) true else default }
        advanceFrame(); assertBadge(null)
        common_updateAvailable(R.string.update_information_header_advanced_mode_hint)
    }

    /** Must be called only once per test */
    private fun setContent(
        allowImmediateCallbacks: Boolean = false,
    ) = setContent(allowImmediateCallbacks) {
        val windowWidthSize = PreviewWindowSize.widthSizeClass
        UpdateScreen(
            navType = NavType.from(windowWidthSize),
            windowWidthSize = windowWidthSize,
            state = RefreshAwareState(false, data),
            onRefresh = { trackCallback("onRefresh") },
            _downloadStatus = DownloadStatus.NotDownloading,
            failureType = NotSet,
            httpFailureCodeAndMessage = { NotSet to "" },
            workProgress = null,
            forceDownloadErrorDialog = false,
            getPrefStr = ::getPrefStr,
            getPrefBool = currentGetPrefBool,
            setSubtitleResId = { trackCallback("setSubtitleResId: $it") },
            enqueueDownload = { trackCallback("enqueueDownload") },
            pauseDownload = { trackCallback("pauseDownload") },
            cancelDownload = { trackCallback("cancelDownload: $it") },
            deleteDownload = { false },
            openInstallGuide = { trackCallback("openInstallGuide") },
            logDownloadError = { trackCallback("logDownloadError") },
            hideDownloadCompleteNotification = { trackCallback("hideDownloadCompleteNotification") },
            showDownloadFailedNotification = { trackCallback("showDownloadFailedNotification") },
        )
    }

    private fun common_upToDate(@StringRes subtitleResId: Int) {
        advanceFrame(); assertBadge(null)

        rule[ErrorStateTestTag].assertDoesNotExist()
        rule[UpdateAvailableContentTestTag].assertDoesNotExist()
        rule[UpToDateContentTestTag].assertExists()

        ensureCallbackInvokedExactlyOnce("setSubtitleResId: $subtitleResId")
    }

    private fun common_updateAvailable(@StringRes subtitleResId: Int) {
        rule[ErrorStateTestTag].assertDoesNotExist()
        rule[UpToDateContentTestTag].assertDoesNotExist()
        rule[UpdateAvailableContentTestTag].assertExists()

        ensureCallbackInvokedExactlyOnce("setSubtitleResId: $subtitleResId")
    }

    private fun assertBadge(value: String?) = assert(MainRoute.Update.badge == value) {
        "Badge did not match. Expected: $value, actual: ${MainRoute.Update.badge}."
    }
}
