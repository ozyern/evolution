package com.ozyern.evolution.ui.onboarding

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.shapes.Capsule
import com.ozyern.evolution.R
import com.ozyern.evolution.icons.Crowdsource
import com.ozyern.evolution.icons.LockOpen
import com.ozyern.evolution.icons.Newsmode
import com.ozyern.evolution.icons.Notes
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.icons.SystemUpdateAlt
import com.ozyern.evolution.models.Device
import com.ozyern.evolution.models.UpdateMethod
import com.ozyern.evolution.ui.SettingsListConfig
import com.ozyern.evolution.ui.common.GroupedColumn
import com.ozyern.evolution.ui.common.GroupedRow
import com.ozyern.evolution.ui.common.GroupedSection
import com.ozyern.evolution.ui.common.groupedCardColor
import com.ozyern.evolution.ui.common.liquid.LiquidToggle
import com.ozyern.evolution.ui.common.rememberState
import com.ozyern.evolution.ui.dialogs.ContributorSheet
import com.ozyern.evolution.ui.dialogs.ModalBottomSheet
import com.ozyern.evolution.ui.settings.DeviceChooser
import com.ozyern.evolution.ui.settings.DeviceSettingsListConfig
import com.ozyern.evolution.ui.settings.MethodChooser
import com.ozyern.evolution.ui.settings.MethodSettingsListConfig
import com.ozyern.evolution.ui.theme.PreviewAppTheme
import com.ozyern.evolution.ui.theme.PreviewGetPrefBool
import com.ozyern.evolution.ui.theme.PreviewGetPrefStr
import com.ozyern.evolution.ui.theme.PreviewThemes
import com.ozyern.evolution.ui.theme.PreviewWindowSize
import com.ozyern.evolution.utils.ContributorUtils

/**
 * Welcome screen, following Apple's onboarding guidelines: a large title, a short list of key
 * features (icon + headline + one line), then only the setup that's actually required, and a
 * single prominent button to continue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    @Suppress("UNUSED_PARAMETER") windowWidthSize: WindowWidthSizeClass,
    scrollBehavior: TopAppBarScrollBehavior,
    deviceConfig: SettingsListConfig<Device>,
    onDeviceSelect: (Device) -> Unit,
    methodConfig: SettingsListConfig<UpdateMethod>,
    onMethodSelect: (UpdateMethod) -> Unit,
    getPrefStr: (key: String, default: String) -> String,
    @Suppress("UNUSED_PARAMETER") getPrefBool: (key: String, default: Boolean) -> Boolean,
    @Suppress("UNUSED_PARAMETER") persistBool: (key: String, value: Boolean) -> Unit,
    onStartAppClick: (contribute: Boolean) -> Unit,
) = GroupedColumn(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
    var contribute by rememberState(true)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterHorizontally)
            .widthIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .testTag(OnboardingScreen_MainColumnTestTag)
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome_title, stringResource(R.string.app_name)),
            fontSize = 34.sp,
            lineHeight = 41.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 32.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Feature(Symbols.SystemUpdateAlt, Color(0xFF007AFF), R.string.onboarding_feature_updates_title, R.string.onboarding_feature_updates_text)
            Feature(Symbols.LockOpen, Color(0xFFFF9500), R.string.onboarding_feature_root_title, R.string.onboarding_feature_root_text)
            Feature(Symbols.Notes, Color(0xFF34C759), R.string.onboarding_feature_notes_title, R.string.onboarding_feature_notes_text)
            Feature(Symbols.Newsmode, Color(0xFFFF3B30), R.string.onboarding_feature_news_title, R.string.onboarding_feature_news_text)
        }

        Spacer(Modifier.height(16.dp))

        GroupedSection(
            header = stringResource(R.string.onboarding_setup_header),
            footer = stringResource(R.string.onboarding_caption),
        ) {
            DeviceChooser(config = deviceConfig, getPrefStr = getPrefStr, onSelect = onDeviceSelect)
            MethodChooser(config = methodConfig, getPrefStr = getPrefStr, onSelect = onMethodSelect)
        }

        if (ContributorUtils.isAtLeastQAndPossiblyRooted) {
            var showSheet by rememberState(false)
            val cardColor = groupedCardColor
            val backdrop = rememberCanvasBackdrop { drawRect(cardColor) }
            GroupedSection {
                GroupedRow(
                    title = stringResource(R.string.contribute_agree),
                    icon = Symbols.Crowdsource,
                    iconTint = Color(0xFFFF2D55),
                    onClick = { contribute = !contribute },
                ) {
                    LiquidToggle(checked = contribute, onCheckedChange = { contribute = it }, backdrop = backdrop)
                }
                GroupedRow(
                    title = stringResource(R.string.contribute_more_info),
                    titleColor = MaterialTheme.colorScheme.primary,
                    onClick = { showSheet = true },
                    modifier = Modifier.testTag(OnboardingScreen_ContributorInfoButtonTestTag)
                )
            }
            if (showSheet) ModalBottomSheet({ showSheet = false }) { ContributorSheet(it) }
        }

        Text(
            text = stringResource(R.string.onboarding_disclaimer),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 16.dp)
                .testTag(OnboardingScreen_DisclaimerTestTag)
        )
    }

    // Primary action pinned to the bottom, like iOS setup flows
    Button(
        onClick = { onStartAppClick(contribute) },
        shape = Capsule(),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .height(52.dp)
    ) {
        Text(stringResource(R.string.onboarding_continue), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Feature(
    icon: ImageVector,
    tint: Color,
    @StringRes titleResId: Int,
    @StringRes textResId: Int,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(36.dp))
    Column(Modifier.weight(1f)) {
        Text(stringResource(titleResId), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = stringResource(textResId),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
    }
}

private const val TAG = "OnboardingScreen"

@VisibleForTesting
const val OnboardingScreen_MainColumnTestTag = TAG + "_MainColumn"

@VisibleForTesting
const val OnboardingScreen_ContributorInfoButtonTestTag = TAG + "_ContributorInfoButton"

@VisibleForTesting
const val OnboardingScreen_DisclaimerTestTag = TAG + "_Disclaimer"

@SuppressLint("VisibleForTests")
@OptIn(ExperimentalMaterial3Api::class)
@PreviewThemes
@Composable
fun PreviewOnboardingScreen() = PreviewAppTheme {
    OnboardingScreen(
        windowWidthSize = PreviewWindowSize.widthSizeClass,
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
        deviceConfig = DeviceSettingsListConfig,
        onDeviceSelect = {},
        methodConfig = MethodSettingsListConfig,
        onMethodSelect = {},
        getPrefStr = PreviewGetPrefStr,
        getPrefBool = PreviewGetPrefBool,
        persistBool = { _, _ -> },
        onStartAppClick = {},
    )
}
