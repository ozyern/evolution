package com.ozyern.evolution.ui.update

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ozyern.evolution.R
import com.ozyern.evolution.icons.CheckCircle
import com.ozyern.evolution.icons.Info
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.internal.settings.KeyUpdateMethod
import com.ozyern.evolution.models.SystemVersionProperties
import com.ozyern.evolution.models.UpdateData
import com.ozyern.evolution.ui.common.ConditionalNavBarPadding
import com.ozyern.evolution.ui.common.GroupedColumn
import com.ozyern.evolution.ui.common.GroupedRow
import com.ozyern.evolution.ui.common.GroupedSection
import com.ozyern.evolution.ui.device.DeviceSoftwareInfo
import com.ozyern.evolution.ui.main.NavType
import com.ozyern.evolution.ui.theme.PreviewAppTheme
import com.ozyern.evolution.ui.theme.PreviewGetPrefStr
import com.ozyern.evolution.ui.theme.PreviewThemes
import com.ozyern.evolution.ui.theme.PreviewWindowSize
import com.ozyern.evolution.ui.theme.positive
import com.ozyern.evolution.utils.UpdateDataVersionFormatter

@Composable
fun UpToDate(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    refreshing: Boolean,
    updateData: UpdateData,
    getPrefStr: (key: String, default: String) -> String,
) {
    val currentOtaVersion = SystemVersionProperties.otaVersion
    val method = getPrefStr(KeyUpdateMethod, "")

    val incomingOtaVersion = updateData.otaVersionNumber
    val isDifferentVersion = incomingOtaVersion != currentOtaVersion
    val showAdvancedModeTip = isDifferentVersion // show advanced mode hint only if OTA versions don't match…
            // …and incoming is newer (older builds can't be installed due to standard Android security measures)
            && UpdateData.getBuildDate(incomingOtaVersion) >= UpdateData.getBuildDate(currentOtaVersion)
            // …and incoming is likely a full ZIP, or the selected update method is "full"
            // (incrementals are only for specific source/target version combos)
            && (updateData.downloadSize >= FullZipLikelyMinSize || method.endsWith("(full)") || method.endsWith("(volledig)"))

    GroupedColumn(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .testTag(UpToDateContentTestTag)
    ) {
        // Keep content at a readable width on tablets/foldables
        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .testTag(UpToDateContent_InfoColumnTestTag)
        ) {
            UpdateHero(
                icon = Symbols.CheckCircle,
                tint = MaterialTheme.colorScheme.positive,
                title = UpdateDataVersionFormatter.getFormattedOsVersion(SystemVersionProperties.osVersion),
                subtitle = stringResource(R.string.update_information_system_is_up_to_date),
                refreshing = refreshing,
            )

            if (showAdvancedModeTip) GroupedSection {
                GroupedRow(
                    title = stringResource(
                        R.string.update_information_banner_advanced_mode_tip,
                        getPrefStr(KeyUpdateMethod, "<UNKNOWN>"),
                    ),
                    icon = Symbols.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                )
            }

            GroupedSection(header = stringResource(R.string.device_information_software_header)) {
                DeviceSoftwareInfo()
            }

            GroupedSection(
                header = stringResource(R.string.update_release_notes),
                footer = if (updateData.isUpdateInformationAvailable) null
                else stringResource(R.string.update_information_no_update_data_available),
            ) {
                ChangelogContainer(
                    refreshing = refreshing,
                    updateData = updateData,
                    isDifferentVersion = isDifferentVersion,
                    showAdvancedModeTip = showAdvancedModeTip,
                    getPrefStr = getPrefStr,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            ConditionalNavBarPadding(navType)
        }
    }
}

private const val FullZipLikelyMinSize = 1048576 * 2500L // 2.5 GB

private const val TAG = "UpToDateContent"

@VisibleForTesting
const val UpToDateContentTestTag = TAG

@VisibleForTesting
const val UpToDateContent_InfoColumnTestTag = TAG + "_InfoColumnTestTag"

@PreviewThemes
@Composable
fun PreviewUpToDate() = PreviewAppTheme {
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    UpToDate(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        refreshing = false,
        updateData = PreviewUpdateData,
        getPrefStr = PreviewGetPrefStr,
    )
}
