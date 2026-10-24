package com.oxygenupdater.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.kyant.shapes.RoundedRectangle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.imageLoader
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.extensions.formatFileSize
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.openAppLocalePage
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.icons.CloudDownload
import com.oxygenupdater.icons.Crowdsource
import com.oxygenupdater.icons.DeleteSweep
import com.oxygenupdater.icons.Language
import com.oxygenupdater.icons.LockOpen
import com.oxygenupdater.icons.Mobile
import com.oxygenupdater.icons.Notifications
import com.oxygenupdater.icons.Palette
import com.oxygenupdater.icons.Policy
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.icons.TripOrigin
import com.oxygenupdater.internal.settings.KeyAdvancedMode
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.Theme
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.GroupedColumn
import com.oxygenupdater.ui.common.GroupedRow
import com.oxygenupdater.ui.common.GroupedSection
import com.oxygenupdater.ui.common.groupedCardColor
import com.oxygenupdater.ui.common.liquid.LiquidToggle
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.AdvancedModeSheet
import com.oxygenupdater.ui.dialogs.ContributorSheet
import com.oxygenupdater.ui.dialogs.LanguageSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.SelectableSheet
import com.oxygenupdater.ui.dialogs.ThemeSheet
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.LocalTheme
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewGetPrefBool
import com.oxygenupdater.ui.theme.PreviewGetPrefStr
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.NotifStatus
import com.oxygenupdater.utils.NotifUtils
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.utils.okHttpCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navType: NavType,
    onContributorEnrollmentChange: (Boolean) -> Unit,
    deviceConfig: SettingsListConfig<Device>,
    onDeviceSelect: (Device) -> Unit,
    methodConfig: SettingsListConfig<UpdateMethod>,
    onMethodSelect: (UpdateMethod) -> Unit,
    onThemeSelect: (Theme) -> Unit,
    pureBlack: Boolean,
    onPureBlackChange: (Boolean) -> Unit,
    onAdvancedModeChange: (Boolean) -> Unit,
    openAboutScreen: () -> Unit,
    getPrefStr: (key: String, default: String) -> String,
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    persistBool: (key: String, value: Boolean) -> Unit,
) = GroupedColumn(
    Modifier
        .verticalScroll(rememberScrollState())
        .testTag(SettingsScreenTestTag)
) {
    GroupedSection(header = stringResource(R.string.preference_header_device)) {
        DeviceChooser(config = deviceConfig, getPrefStr = getPrefStr, onSelect = onDeviceSelect)
        MethodChooser(config = methodConfig, getPrefStr = getPrefStr, onSelect = onMethodSelect)
        Notifications()
    }

    GroupedSection(header = stringResource(R.string.preference_header_ui)) {
        Theme(onSelect = onThemeSelect)
        SettingsSwitchItem(
            checked = pureBlack,
            onCheckedChange = onPureBlackChange,
            icon = Symbols.TripOrigin,
            iconTint = IconGray,
            titleResId = R.string.settings_pure_black,
        )
        Language()
    }

    GroupedSection(header = stringResource(R.string.preference_header_advanced)) {
        AdvancedMode(getPrefBool = getPrefBool, onChange = onAdvancedModeChange)
        // Contributing OTA URLs is available only on (possibly) rooted devices
        BecomeContributor(
            getPrefBool = getPrefBool,
            onContributorEnrollmentChange = onContributorEnrollmentChange,
        )
        ClearCache()
    }

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    GroupedSection(
        header = stringResource(R.string.preference_header_about),
        footer = stringResource(R.string.settings_footer_free),
    ) {
        SettingsItem(
            // Use Chrome Custom Tabs to open the privacy policy link
            onClick = { customTabIntent.launch(context, "https://oxygenupdater.com/privacy/") },
            icon = Symbols.Policy,
            iconTint = IconBlue,
            titleResId = R.string.label_privacy_policy,
            subtitle = null,
        )

        GroupedRow(
            title = stringResource(R.string.app_name),
            value = "v${BuildConfig.VERSION_NAME}",
            chevron = true,
            onClick = openAboutScreen,
            leading = {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedRectangle(8.dp))
                )
            },
        )
    }

    Spacer(Modifier.height(24.dp))
    ConditionalNavBarPadding(navType)
}

@Composable
fun DeviceChooser(
    config: SettingsListConfig<Device>,
    getPrefStr: (key: String, default: String) -> String,
    onSelect: (Device) -> Unit,
) {
    var showSheet by rememberState(false)

    val deviceSelectionEnabled = config.list.isNotEmpty()
    @SuppressLint("PrivateResource") val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (deviceSelectionEnabled) {
        getPrefStr(KeyDevice, notSelected)
    } else stringResource(R.string.summary_please_wait)

    SettingsItem(
        onClick = { showSheet = true },
        icon = Symbols.Mobile,
        iconTint = IconBlue,
        titleResId = R.string.settings_device,
        subtitle = subtitle,
        subtitleIsError = subtitle == notSelected,
        enabled = deviceSelectionEnabled,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            config = config,
            titleResId = R.string.settings_search_devices,
            captionResId = R.string.onboarding_device_chooser_caption,
            filter = DeviceSearchFilter,
            onClick = onSelect,
        )
    }
}

@Composable
fun MethodChooser(
    config: SettingsListConfig<UpdateMethod>,
    getPrefStr: (key: String, default: String) -> String,
    onSelect: (UpdateMethod) -> Unit,
) {
    var showSheet by rememberState(false)

    val methodSelectionEnabled = config.list.isNotEmpty()
    @SuppressLint("PrivateResource") val notSelected = stringResource(androidx.compose.ui.R.string.not_selected)
    val subtitle = if (methodSelectionEnabled) {
        getPrefStr(KeyUpdateMethod, notSelected)
    } else stringResource(R.string.summary_update_method)

    SettingsItem(
        onClick = { showSheet = true },
        icon = Symbols.CloudDownload,
        iconTint = IconGreen,
        titleResId = R.string.settings_update_method,
        subtitle = subtitle,
        subtitleIsError = subtitle == notSelected,
        enabled = methodSelectionEnabled,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            config = config,
            titleResId = R.string.settings_update_method,
            captionResId = R.string.onboarding_method_chooser_caption,
            onClick = onSelect,
        )
    }
}

@Composable
private fun BecomeContributor(
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    onContributorEnrollmentChange: (Boolean) -> Unit,
) {
    var showSheet by rememberState(false)
    if (ContributorUtils.isAtLeastQAndPossiblyRooted) SettingsItem(
        onClick = { showSheet = true },
        icon = Symbols.Crowdsource,
        iconTint = IconPink,
        titleResId = R.string.contribute,
        subtitle = stringResource(R.string.settings_contribute_label),
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) {
        ContributorSheet(
            hide = it,
            getPrefBool = getPrefBool,
            confirm = onContributorEnrollmentChange,
        )
    }
}

@Composable
private fun Notifications() {
    val context = LocalContext.current
    val runningInPreview = LocalInspectionMode.current
    var notifStatus by rememberState(if (runningInPreview) NotifStatus() else NotifUtils.toNotifStatus(context))

    if (!runningInPreview) LifecycleResumeEffect(context) {
        // Check again in case user changed settings and came back to the app
        notifStatus = NotifUtils.toNotifStatus(context)
        onPauseOrDispose {}
    }

    val disabled = notifStatus.disabled
    val subtitleIsError: Boolean
    val notifSummary = if (disabled.isNullOrEmpty()) {
        subtitleIsError = true
        stringResource(R.string.summary_off)
    } else if (disabled.none { it }) {
        subtitleIsError = false
        stringResource(R.string.summary_on)
    } else stringResource(R.string.summary_important_notifications_disabled) + buildString {
        disabled.forEachIndexed { index, flag ->
            if (!flag) return@forEachIndexed
            when (index) {
                0 -> R.string.update_notification_channel_name
                1 -> R.string.news_notification_channel_name
                2 -> R.string.download_status_notification_channel_name
                else -> throw ArrayIndexOutOfBoundsException(index)
            }.let { append("\n• " + stringResource(it)) }
        }
    }.also {
        subtitleIsError = true
    }

    SettingsItem(
        onClick = {
            val packageName = context.packageName
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            // Works only for API 21+ (Lollipop), which happens to be the min API
            else Intent(
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            ).putExtra("app_package", packageName).putExtra("app_uid", context.applicationInfo.uid)
            context.startActivity(intent)
        },
        icon = Symbols.Notifications,
        iconTint = IconRed,
        titleResId = R.string.preference_header_notifications,
        subtitle = notifSummary,
        subtitleIsError = subtitleIsError,
    )
}

@Composable
private fun Theme(onSelect: (Theme) -> Unit) {
    var showSheet by rememberState(false)
    SettingsItem(
        onClick = { showSheet = true },
        icon = Symbols.Palette,
        iconTint = IconIndigo,
        titleResId = R.string.label_theme,
        subtitle = stringResource(LocalTheme.current.titleResId),
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        ThemeSheet {
            onSelect(it)
            hide()
        }
    }
}

@Composable
private fun Language() {
    val selectedLocale = currentLocale()
    val language = remember(selectedLocale) {
        selectedLocale.displayName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(selectedLocale) else it.toString()
        }
    }

    var showSheet by rememberState(false)
    val context = LocalContext.current
    SettingsItem(
        onClick = {
            // Use our own sheet below Android 13
            showSheet = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true else try {
                // Otherwise delegate to system API on Android 13+ if possible
                context.openAppLocalePage()
                false
            } catch (e: Exception) {
                logInfo("SettingsScreen", "openAppLocalePage failed", e)
                true // fallback just in case
            }
        },
        icon = Symbols.Language,
        iconTint = IconTeal,
        titleResId = R.string.label_language,
        subtitle = language,
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        LanguageSheet({
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it))
            hide()
        }, selectedLocale)
    }
}

@Composable
private fun AdvancedMode(
    getPrefBool: (key: String, default: Boolean) -> Boolean,
    onChange: (Boolean) -> Unit,
) {
    var showSheet by rememberState(false)

    // Maintain state if user leaves this screen, then comes back to it
    var advancedMode by rememberState(getPrefBool(KeyAdvancedMode, false))
    SettingsSwitchItem(
        checked = advancedMode,
        onCheckedChange = {
            onChange(it)
            advancedMode = it
        },
        icon = Symbols.LockOpen,
        iconTint = IconOrange,
        titleResId = R.string.settings_advanced_mode,
        showWarning = { showSheet = true },
    )

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        AdvancedModeSheet {
            onChange(it)
            advancedMode = it
            hide()
        }
    }
}

@Composable
private fun ClearCache() {
    val context = LocalContext.current
    val coilImageLoader = context.imageLoader

    var recomputationKey by rememberState(false)
    var size by rememberState(
        recomputationKey,
        try {
            // Only count disk cache sizes
            (okHttpCache?.size() ?: 0L) + (coilImageLoader.diskCache?.size ?: 0L)
        } catch (_: Exception) {
            0L
        }
    )

    // For running IO-intensive operations
    val scope = rememberCoroutineScope { Dispatchers.IO }
    SettingsItem(
        onClick = {
            try {
                /**
                 * https://square.github.io/okhttp/features/caching/#pruning-the-cache
                 * We choose to only clear cache for API calls and images. Other caches, e.g. WebView
                 * are not deleted. If this decision changes in the future, call `okHttpCache?.delete()`
                 * to clear everything in the cache directory. For posterity, WebView cache can be
                 * programmatically cleared via `WebView(context).clearCache(true)`.
                 */
                scope.launch {
                    okHttpCache?.evictAll()
                    coilImageLoader.diskCache?.clear()
                    // coilImageLoader.memoryCache?.clear() // is this required?
                }

                // Toggle to let the `size` calculation run again
                recomputationKey = !recomputationKey

                context.showToast(androidx.appcompat.R.string.abc_action_mode_done)
            } catch (e: Exception) {
                logInfo(SettingsScreenTestTag, "Error when clearing cache", e)
            }
        },
        icon = Symbols.DeleteSweep,
        iconTint = IconGray,
        titleResId = R.string.settings_clear_cache,
        subtitle = context.formatFileSize(size),
        enabled = size != 0L,
    )
}

@Composable
fun SettingsSwitchItem(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    icon: ImageVector,
    @StringRes titleResId: Int,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    showWarning: (() -> Unit)? = null,
) {
    val checkedChange by rememberUpdatedState<(checked: Boolean) -> Unit> {
        // Handoff to BottomSheet if necessary, pref will update based on user choice (Cancel/Enable)
        if (it && showWarning != null) showWarning() else {
            onCheckedChange(it)
        }
    }

    // The glass thumb refracts the card behind it
    val cardColor = groupedCardColor
    val backdrop = rememberCanvasBackdrop { drawRect(cardColor) }
    GroupedRow(
        title = stringResource(titleResId),
        icon = icon,
        iconTint = iconTint,
        subtitle = subtitle,
        onClick = { checkedChange(!checked) },
        modifier = Modifier.windowInsetsPadding(
            // Leave space for 2/3-button nav bar in landscape mode
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
        ),
    ) {
        LiquidToggle(checked = checked, onCheckedChange = checkedChange, backdrop = backdrop)
    }
}

/**
 * iOS-style settings row. Short, single-line [subtitle]s are shown as a trailing value
 * (like iOS "Detail" cells); longer ones, and errors, are shown below the title.
 */
@Composable
@NonRestartableComposable
fun SettingsItem(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes titleResId: Int,
    subtitle: String?,
    subtitleIsError: Boolean = false,
    enabled: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    val asValue = subtitle != null && !subtitleIsError && subtitle.length <= 18 && '\n' !in subtitle
    GroupedRow(
        title = stringResource(titleResId),
        icon = icon,
        iconTint = iconTint,
        subtitle = if (asValue) null else subtitle,
        subtitleIsError = subtitleIsError,
        value = if (asValue) subtitle else null,
        chevron = content == null,
        enabled = enabled,
        onClick = onClick,
        trailing = content,
    )
}

// Apple system colours, used for setting icons
private val IconBlue = Color(0xFF007AFF)
private val IconGreen = Color(0xFF34C759)
private val IconRed = Color(0xFFFF3B30)
private val IconOrange = Color(0xFFFF9500)
private val IconIndigo = Color(0xFF5856D6)
private val IconTeal = Color(0xFF30B0C7)
private val IconPink = Color(0xFFFF2D55)
private val IconGray = Color(0xFF8E8E93)

@VisibleForTesting
val DeviceSearchFilter: (Device, String) -> Boolean = { device, query ->
    device.name?.contains(query, ignoreCase = true) == true
            || device.productNames.any { it.contains(query, ignoreCase = true) }
}

@VisibleForTesting
const val SettingsScreenTestTag = "SettingsScreen"

@VisibleForTesting
val DeviceSettingsListConfig = SettingsListConfig(
    list = listOf(
        Device(
            id = 1,
            name = "OnePlus 7 Pro",
            productNames = listOf("OnePlus7Pro,GM1917"),
            enabled = true,
        ),
        Device(
            id = 2,
            name = "OnePlus 8T",
            productNames = listOf("OnePlus8T,KB2001"),
            enabled = true,
        ),
    ),
    recommendedId = 2,
    selectedId = 1,
)

@VisibleForTesting
val MethodSettingsListConfig = SettingsListConfig(
    list = listOf(
        UpdateMethod(
            id = 1,
            name = "Stable (full)",
            recommendedForRootedDevice = true,
            recommendedForNonRootedDevice = false,
            supportsRootedDevice = true,
        ),
        UpdateMethod(
            id = 2,
            name = "Stable (incremental)",
            recommendedForRootedDevice = false,
            recommendedForNonRootedDevice = true,
            supportsRootedDevice = false,
        )
    ),
    recommendedId = 2,
    selectedId = 1,
)

@PreviewThemes
@Composable
fun PreviewSettingsScreen() = PreviewAppTheme {
    SettingsScreen(
        navType = NavType.BottomBar,
        onContributorEnrollmentChange = {},
        deviceConfig = DeviceSettingsListConfig,
        onDeviceSelect = {},
        methodConfig = MethodSettingsListConfig,
        onMethodSelect = {},
        onThemeSelect = {},
        pureBlack = false,
        onPureBlackChange = {},
        onAdvancedModeChange = {},
        openAboutScreen = {},
        getPrefStr = PreviewGetPrefStr,
        getPrefBool = PreviewGetPrefBool,
        persistBool = { _, _ -> },
    )
}
