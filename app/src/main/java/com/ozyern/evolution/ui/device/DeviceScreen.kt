package com.ozyern.evolution.ui.device

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Build.UNKNOWN
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.ozyern.evolution.R
import com.ozyern.evolution.icons.Android
import com.ozyern.evolution.icons.AspectRatio
import com.ozyern.evolution.icons.Commit
import com.ozyern.evolution.icons.DeveloperBoard
import com.ozyern.evolution.icons.Download
import com.ozyern.evolution.icons.Error
import com.ozyern.evolution.icons.Memory
import com.ozyern.evolution.icons.MobileCancel
import com.ozyern.evolution.icons.MobileCheck
import com.ozyern.evolution.icons.MobileInfo
import com.ozyern.evolution.icons.MobileLockPortrait
import com.ozyern.evolution.icons.MobileQuestion
import com.ozyern.evolution.icons.PhotoCamera
import com.ozyern.evolution.icons.Security
import com.ozyern.evolution.icons.Speed
import com.ozyern.evolution.icons.Steppers
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.icons.TripOrigin
import com.ozyern.evolution.models.ChinaDevices
import com.ozyern.evolution.models.Device
import com.ozyern.evolution.models.DeviceOsSpec
import com.ozyern.evolution.models.SystemVersionProperties
import com.ozyern.evolution.ui.common.ConditionalNavBarPadding
import com.ozyern.evolution.ui.common.GroupedColumn
import com.ozyern.evolution.ui.common.GroupedRow
import com.ozyern.evolution.ui.common.GroupedSection
import com.ozyern.evolution.ui.common.rememberState
import com.ozyern.evolution.ui.common.tintedPainter
import com.ozyern.evolution.ui.currentLocale
import com.ozyern.evolution.ui.main.NavType
import com.ozyern.evolution.ui.theme.PreviewAppTheme
import com.ozyern.evolution.ui.theme.PreviewThemes
import com.ozyern.evolution.ui.theme.PreviewWindowSize
import com.ozyern.evolution.utils.UpdateDataVersionFormatter
import com.ozyern.evolution.utils.logInfo
import java.text.NumberFormat
import java.util.MissingResourceException
import kotlin.math.roundToLong

/**
 * Used when the server doesn't (yet) list this device, e.g. Chinese variants. Prefers the
 * OPPO/OnePlus market name (e.g. "OnePlus 13"), and marks mainland China ROMs as such.
 */
val DefaultDeviceName = (ChinaDevices.current ?: SystemVersionProperties.deviceMarketName.ifEmpty {
    "${Build.MANUFACTURER} ${Build.DEVICE}"
}).let { if (SystemVersionProperties.isChinaBuild) "$it (China)" else it }

/**
 * Styled after Apple's "Device Info" pages: a centred device image with its name & model,
 * followed by grouped cards of label/value rows.
 */
@Composable
fun DeviceScreen(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    deviceName: String,
    deviceOsSpec: DeviceOsSpec?,
    deviceMismatchStatus: Triple<Boolean, String, String>?,
    cpuFrequencies: ScatterMap<Float, Int>,
    openCnRoms: () -> Unit,
) = GroupedColumn(
    Modifier
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .testTag(DeviceScreen_ScrollableColumnTestTag)
) {
    // Keep cards at a readable width on tablets/foldables
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .widthIn(max = 640.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        DeviceImage(
            deviceName = deviceName,
            deviceOsSpec = deviceOsSpec,
            size = if (windowWidthSize == WindowWidthSizeClass.Expanded) 192.dp else 160.dp,
        )

        Text(
            text = deviceName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                .testTag(DeviceScreen_NameTestTag)
        )

        SelectionContainer {
            Text(
                text = Build.MODEL,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 8.dp)
                    .testTag(DeviceScreen_ModelTestTag)
            )
        }

        DeviceSupportSection(deviceOsSpec = deviceOsSpec, deviceMismatchStatus = deviceMismatchStatus)

        // The update server has no China firmware, so offer it from the community ROM archive instead
        GroupedSection(header = stringResource(R.string.about_firmware_header)) {
            GroupedRow(
                title = stringResource(R.string.cn_roms_title),
                subtitle = stringResource(R.string.cn_roms_row_subtitle),
                icon = Symbols.Download,
                iconTint = Color(0xFFFF3B30),
                chevron = true,
                onClick = openCnRoms,
                modifier = Modifier.testTag(DeviceScreen_CnRomsTestTag)
            )
        }

        GroupedSection(header = stringResource(R.string.device_information_software_header)) {
            DeviceSoftwareInfo()
        }

        GroupedSection(header = stringResource(R.string.device_information_hardware_header)) {
            DeviceHardwareInfo(cpuFrequencies)
        }

        Spacer(Modifier.height(24.dp))
        ConditionalNavBarPadding(navType)
    }
}

/** Like "Find My Mac: On" — whether this app can deliver updates to this device */
@Composable
private fun DeviceSupportSection(
    deviceOsSpec: DeviceOsSpec?,
    deviceMismatchStatus: Triple<Boolean, String, String>?,
) {
    val supported = deviceOsSpec == DeviceOsSpec.SupportedDeviceAndOs
    val statusText = stringResource(remember(deviceOsSpec) {
        when (deviceOsSpec) {
            DeviceOsSpec.SupportedDeviceAndOs -> R.string.device_information_supported_oxygen_os
            DeviceOsSpec.CarrierExclusiveOxygenOs -> R.string.device_information_carrier_exclusive_oxygen_os
            DeviceOsSpec.UnsupportedDevice -> R.string.device_information_unsupported_oxygen_os
            else -> R.string.device_information_unsupported_os
        }
    })
    val mismatch = deviceMismatchStatus?.takeIf { it.first }?.let {
        stringResource(R.string.incorrect_device_warning_message, it.second, it.third)
    }

    var showUnsupportedDialog by rememberState(false)
    if (!supported) deviceOsSpec?.let {
        UnsupportedDeviceOsSpecDialog(showUnsupportedDialog, { showUnsupportedDialog = false }, it)
    }

    // China variants are recognised locally, but the update server has no China firmware
    val footer = if (!supported && ChinaDevices.current != null) {
        stringResource(R.string.device_china_not_on_server)
    } else statusText
    GroupedSection(footer = footer) {
        GroupedRow(
            title = stringResource(R.string.update_information_header),
            value = stringResource(if (supported) R.string.device_support_supported else R.string.device_support_not_supported),
            chevron = !supported,
            onClick = if (supported) null else ({ showUnsupportedDialog = true }),
            modifier = Modifier.testTag(DeviceScreen_SupportStatusTestTag)
        )
    }

    if (mismatch != null) Text(
        text = mismatch,
        color = MaterialTheme.colorScheme.error,
        fontSize = 13.sp,
        modifier = Modifier
            .padding(start = 32.dp, end = 32.dp, top = 8.dp)
            .testTag(DeviceScreen_MismatchTextTestTag)
    )
}

@Composable
private fun DeviceImage(deviceName: String, deviceOsSpec: DeviceOsSpec?, size: Dp) {
    val notSupported = deviceOsSpec != DeviceOsSpec.SupportedDeviceAndOs
    var showUnsupportedDialog by rememberState(false)
    if (notSupported) deviceOsSpec?.let {
        UnsupportedDeviceOsSpecDialog(showUnsupportedDialog, { showUnsupportedDialog = false }, it)
    }

    Box(Modifier.clickable(enabled = notSupported) { showUnsupportedDialog = true }) {
        val requiredSizeModifier = Modifier.requiredSize(size)

        val context = LocalContext.current
        val defaultImage = tintedPainter(
            painter = rememberVectorPainter(
                when (deviceOsSpec) {
                    DeviceOsSpec.SupportedDeviceAndOs -> Symbols.MobileCheck // device frame with tick mark
                    DeviceOsSpec.CarrierExclusiveOxygenOs -> Symbols.MobileLockPortrait // device frame with locked icon
                    DeviceOsSpec.UnsupportedDevice -> Symbols.MobileCancel // device frame with cancel mark
                    // Covers UnsupportedDeviceAndOs
                    else -> Symbols.MobileQuestion // device frame with question mark
                }
            ),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
        )
        AsyncImage(
            model = deviceName.let {
                val sizePx = LocalDensity.current.run { size.roundToPx() }
                remember(it, sizePx) {
                    ImageRequest.Builder(context)
                        .data(Device.constructImageUrl(it))
                        .size(sizePx)
                        .build()
                }
            },
            contentDescription = stringResource(R.string.device_information_image_description),
            placeholder = defaultImage,
            error = defaultImage,
            modifier = requiredSizeModifier.testTag(DeviceScreen_ImageTestTag)
        )

        if (notSupported) {
            Box(requiredSizeModifier.background(MaterialTheme.colorScheme.surface.copy(alpha = .75f)))
            Icon(
                imageVector = Symbols.Error,
                contentDescription = stringResource(R.string.icon),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag(DeviceScreen_NotSupportedIconTestTag)
            )
        }
    }
}

/** Software rows, meant to be placed inside a [GroupedSection] */
@Composable
fun DeviceSoftwareInfo() {

    Item(
        icon = Symbols.Android,
        titleResId = R.string.device_information_os_version,
        text = Build.VERSION.RELEASE,
    )

    SystemVersionProperties.osVersion.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Symbols.TripOrigin,
            titleResId = R.string.device_information_oxygen_os_version,
            text = UpdateDataVersionFormatter.getFormattedOsVersion(it),
        )
    }

    SystemVersionProperties.otaVersion.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Symbols.TripOrigin,
            titleResId = R.string.device_information_oxygen_os_ota_version,
            text = it
        )
    }

    Item(
        icon = Symbols.Steppers,
        titleResId = R.string.device_information_incremental_os_version,
        text = Build.VERSION.INCREMENTAL,
    )

    SystemVersionProperties.securityPatchDate.takeIf { it != UNKNOWN }?.let {
        Item(
            icon = Symbols.Security,
            titleResId = R.string.device_information_patch_level_version,
            text = it,
        )
    }

    // Kernel version
    remember {
        try {
            // This is displayed in Settings › About device › Version, for example:
            // 6.6.89-android15-8-g5a0ffb447c1d-ab13771415-4k. Note: `ro.build.kernel.id` is 6.6.89;
            // `ro.kernel.version` is 6.6. FYI the file `/proc/version` contains far more information,
            // including the specific https://android.googlesource.com/toolchain/llvm-project commit.
            val process = Runtime.getRuntime().exec("uname -r")
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            process.destroy()
            output.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null // ignore
        }
    }?.let {
        Item(
            icon = Symbols.Commit,
            titleResId = R.string.device_information_kernel_version,
            text = it,
        )
    }
}

/** Hardware rows, meant to be placed inside a [GroupedSection] */
@Composable
private fun DeviceHardwareInfo(cpuFrequencies: ScatterMap<Float, Int>) {
    val context = LocalContext.current
    val bytes = remember(context) {
        try {
            Formatter.formatShortFileSize(context, getRamBytes(context))
        } catch (_: MissingResourceException) {
            // Rare crash, probably due to a modified Android system.
            // Fallback to manually computing to display in GB. (not locale-specific)
            "${getRamBytes(context) / 1073741824} GB"
        }
    }
    if (bytes.isNotEmpty()) Item(
        icon = Symbols.Memory,
        titleResId = R.string.device_information_amount_of_memory,
        text = bytes,
    )

    Item(
        icon = Symbols.DeveloperBoard,
        titleResId = R.string.device_information_system_on_a_chip,
        text = Build.BOARD.let { board ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val socManufacturer = Build.SOC_MANUFACTURER
                val socModel = Build.SOC_MODEL
                val validManufacturer = socManufacturer != UNKNOWN
                val validModel = socModel != UNKNOWN

                if (validManufacturer && validModel) {
                    "$socManufacturer $socModel ($board)"
                } else if (validManufacturer) {
                    "$socManufacturer ($board)"
                } else if (validModel) {
                    "$socModel ($board)"
                } else board
            } else board
        },
    )

    if (cpuFrequencies.isNotEmpty()) {
        val locale = currentLocale()
        // Format according to locale (decimal vs comma)
        val formatted = remember(locale) {
            val instance = NumberFormat.getInstance(locale)
            var validCount = 0
            buildString {
                cpuFrequencies.forEach { frequency, count ->
                    val formatted = try {
                        @SuppressLint("LocalContextGetResourceValueCall")
                        context.getString(
                            R.string.device_information_gigahertz,
                            instance.format(frequency).ifEmpty { return@forEach }
                        )
                    } catch (_: Exception) {
                        return@forEach
                    }
                    append("• ").append(formatted)
                    append(" (").append(count).append(')')
                    appendLine()
                    validCount++
                }
            }.trimEnd().let {
                // Remove bullet prefix if single line (single-core or multi-core with the same GHz values)
                if (validCount == 1) it.removePrefix("• ") else it
            }
        }
        if (formatted.isNotEmpty()) Item(
            icon = Symbols.Speed,
            titleResId = R.string.device_information_cpu_frequency,
            text = formatted,
        )
    }

    // Screen size(s) in in/cm
    SystemVersionProperties.screenSizes.takeIf { it.isNotEmpty() }?.let {
        Item(
            icon = Symbols.AspectRatio,
            titleResId = R.string.device_information_screen_size,
            text = it,
        )
    }

    // Camera megapixel counts
    SystemVersionProperties.cameraMegapixelCounts.takeIf { it.isNotEmpty() }?.let {
        Item(
            icon = Symbols.PhotoCamera,
            titleResId = R.string.device_information_cameras,
            text = it,
        )
    }

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        // Not shown on Android 8/O & above because it requires too many permissions
        Item(
            icon = Symbols.MobileInfo,
            titleResId = R.string.device_information_serial_number,
            text = Build.SERIAL,
        )
    }
}

/** Label on the leading edge, value on the trailing edge; long/multi-line values go below */
@Composable
@NonRestartableComposable
private fun Item(
    @Suppress("UNUSED_PARAMETER") icon: ImageVector,
    @StringRes titleResId: Int,
    text: String,
) {
    val asValue = text.length <= 24 && '\n' !in text
    GroupedRow(
        title = stringResource(titleResId),
        value = if (asValue) text else null,
        subtitle = if (asValue) null else text,
    )
}

private fun getRamBytes(context: Context) = try {
    val totalMem = ActivityManager.MemoryInfo().apply {
        context.getSystemService<ActivityManager>()?.getMemoryInfo(this)
    }.totalMem

    // Round up
    var approxRam = (totalMem / 1_000_000_000.toDouble()).roundToLong()
    // RAM can never be an odd number in OP devices
    if (approxRam % 2 == 1L) approxRam++

    // `mi.totalMem` is in bytes, but since we're using it in android.text.format.Formatter.formatShortFileSize,
    // we need to make sure to use SI units across all Android versions
    // The version check is required because `formatShortFileSize` uses SI units only in Oreo and above.
    // It uses IEC units pre-Oreo.
    approxRam * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 1_000_000_000L else 1_073_741_824
} catch (e: Exception) {
    logInfo("DeviceScreen", "Memory information is unavailable due to error", e)
    0L
}

private const val TAG = "DeviceScreen"

@VisibleForTesting
const val DeviceScreen_ScrollableColumnTestTag = TAG + "_ScrollableColumn"

@VisibleForTesting
const val DeviceScreen_NameTestTag = TAG + "_Name"

@VisibleForTesting
const val DeviceScreen_ModelTestTag = TAG + "_Model"

@VisibleForTesting
const val DeviceScreen_SupportStatusTestTag = TAG + "_SupportStatus"

@VisibleForTesting
const val DeviceScreen_ImageTestTag = TAG + "_Image"

@VisibleForTesting
const val DeviceScreen_NotSupportedIconTestTag = TAG + "_NotSupportedIcon"

@VisibleForTesting
const val DeviceScreen_MismatchTextTestTag = TAG + "_MismatchText"

@VisibleForTesting
const val DeviceScreen_CnRomsTestTag = TAG + "_CnRoms"

@PreviewThemes
@Composable
fun PreviewDeviceScreen() = PreviewAppTheme {
    val name = DefaultDeviceName
    val windowWidthSize = PreviewWindowSize.widthSizeClass
    DeviceScreen(
        navType = NavType.from(windowWidthSize),
        windowWidthSize = windowWidthSize,
        deviceName = name,
        deviceOsSpec = DeviceOsSpec.SupportedDeviceAndOs,
        deviceMismatchStatus = Triple(false, name, name),
        cpuFrequencies = emptyScatterMap(),
        openCnRoms = {},
    )
}
