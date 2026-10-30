package com.oxygenupdater.ui.cnroms

import android.app.DownloadManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.text.format.Formatter
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.shapes.RoundedRectangle
import com.oxygenupdater.R
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.launch
import com.oxygenupdater.extensions.openLink
import com.oxygenupdater.extensions.rememberCustomTabsIntent
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.icons.Download
import com.oxygenupdater.icons.Error
import com.oxygenupdater.icons.Link
import com.oxygenupdater.icons.Mobile
import com.oxygenupdater.icons.Notes
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.models.CnRomBuild
import com.oxygenupdater.models.CnRomDevice
import com.oxygenupdater.models.CnRomParser
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.SettingsListConfig
import com.oxygenupdater.ui.common.CapsuleButton
import com.oxygenupdater.ui.common.ConditionalNavBarPadding
import com.oxygenupdater.ui.common.GroupedFooter
import com.oxygenupdater.ui.common.GroupedHeader
import com.oxygenupdater.ui.common.GroupedRow
import com.oxygenupdater.ui.common.GroupedSection
import com.oxygenupdater.ui.common.PillLabel
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.groupedBackground
import com.oxygenupdater.ui.common.groupedCardColor
import com.oxygenupdater.ui.common.groupedCell
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.dialogs.SelectableSheet
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun CnRomsScreen(navType: NavType, viewModel: CnRomsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    CnRomsScreen(
        navType = navType,
        state = state,
        onRefresh = { viewModel.refresh() },
        onSelectDevice = viewModel::select,
        onOpenBuild = viewModel::open,
    )

    detail?.let {
        ModalBottomSheet(viewModel::closeDetail) { _ ->
            CnRomDetailSheet(
                state = it,
                onRetry = viewModel::retryDetail,
                fetchDownloadUrl = viewModel::fetchDownloadUrl,
            )
        }
    }
}

/**
 * China firmware browser, styled like iOS Settings › General › Software Update's list views:
 * a device picker card, then the device's builds (newest first) in an inset grouped list.
 */
@VisibleForTesting
@Composable
fun CnRomsScreen(
    navType: NavType,
    state: CnRomsState,
    onRefresh: () -> Unit,
    onSelectDevice: (CnRomDevice) -> Unit,
    onOpenBuild: (index: Int) -> Unit,
) = Box(
    Modifier
        .fillMaxSize()
        .background(groupedBackground)
) {
    PullRefresh(
        state = RefreshAwareState(state.refreshing, state.devices),
        shouldShowProgressIndicator = { it.isEmpty() },
        onRefresh = onRefresh,
    ) {
        if (state.error) {
            ErrorContent(navType = navType, onRetry = onRefresh)
            return@PullRefresh
        }

        val selected = state.selected
        val installed = remember { Build.DISPLAY }
        LazyColumn(
            Modifier
                .fillMaxSize()
                .testTag(CnRomsScreen_LazyColumnTestTag)
        ) {
            item(key = "device") {
                DeviceSection(state = state, onSelect = onSelectDevice)
            }

            if (selected != null) {
                item(key = "header") { GroupedHeader(stringResource(R.string.cn_roms_builds_header), Modifier.padding(horizontal = 16.dp)) }

                val lastIndex = selected.builds.lastIndex
                itemsIndexed(selected.builds, key = { _, it -> it }) { index, label ->
                    GroupedRow(
                        title = CnRomParser.versionOf(label),
                        subtitle = CnRomParser.detailsOf(label),
                        chevron = true,
                        onClick = { onOpenBuild(index) },
                        trailing = {
                            // Build.DISPLAY is e.g. "PJZ110_16.0.3.500(CN01)" on OnePlus/OPPO China ROMs
                            if (label == installed) PillLabel(stringResource(R.string.cn_roms_installed), Color(0xFF34C759))
                            else if (index == 0) PillLabel(stringResource(R.string.cn_roms_latest))
                        },
                        modifier = Modifier
                            .groupedCell(first = index == 0, last = index == lastIndex)
                            .testTag(CnRomsScreen_BuildRowTestTag)
                    )
                }
            }

            item(key = "footer") {
                GroupedFooter(
                    text = stringResource(R.string.cn_roms_source_footer),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
                ConditionalNavBarPadding(navType)
            }
        }
    }
}

@Composable
private fun DeviceSection(state: CnRomsState, onSelect: (CnRomDevice) -> Unit) {
    var showSheet by rememberState(false)
    val selected = state.selected
    val isThisDevice = selected != null && selected.key == state.thisDeviceKey

    GroupedSection(
        footer = if (state.thisDeviceKey == null) stringResource(R.string.cn_roms_no_match) else null,
    ) {
        GroupedRow(
            title = stringResource(R.string.cn_roms_device),
            icon = Symbols.Mobile,
            iconTint = Color(0xFF007AFF),
            subtitle = if (isThisDevice) stringResource(R.string.cn_roms_this_device) else null,
            value = selected?.name ?: stringResource(R.string.cn_roms_choose_device),
            chevron = true,
            onClick = { showSheet = true },
            modifier = Modifier.testTag(CnRomsScreen_DeviceRowTestTag)
        )
    }

    if (showSheet) ModalBottomSheet({ showSheet = false }) { hide ->
        SelectableSheet(
            hide = hide,
            config = SettingsListConfig(
                list = state.devices,
                recommendedId = state.devices.firstOrNull { it.key == state.thisDeviceKey }?.id ?: -1L,
                selectedId = selected?.id ?: -1L,
            ),
            titleResId = R.string.cn_roms_choose_device,
            captionResId = R.string.cn_roms_device_picker_caption,
            filter = CnRomDeviceSearchFilter,
            onClick = onSelect,
        )
    }
}

@Composable
private fun ErrorContent(navType: NavType, onRetry: () -> Unit) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(32.dp)
) {
    Icon(Symbols.Error, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
    Text(
        text = stringResource(R.string.cn_roms_error_title),
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 16.dp)
    )
    Text(
        text = stringResource(R.string.cn_roms_error_text),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 17.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
    )
    CapsuleButton(text = stringResource(R.string.cn_roms_retry), onClick = onRetry, filled = true)
    ConditionalNavBarPadding(navType)
}

/** Build details, in an iOS-style sheet: header, info rows, an anti-rollback warning, and actions */
@Composable
private fun CnRomDetailSheet(
    state: CnRomDetailState,
    onRetry: () -> Unit,
    fetchDownloadUrl: (onResult: (String?) -> Unit) -> Unit,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(bottom = 24.dp)
        .testTag(CnRomsScreen_DetailSheetTestTag)
) {
    Text(
        text = state.device.name,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
    )
    Text(
        text = CnRomParser.versionOf(state.label),
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Text(
        text = state.label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )

    val build = state.build
    if (build == null) {
        if (state.error) {
            GroupedFooter(stringResource(R.string.cn_roms_detail_error))
            Spacer(Modifier.height(12.dp))
            CapsuleButton(text = stringResource(R.string.cn_roms_retry), onClick = onRetry)
        } else Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.cn_roms_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return@Column
    }

    BuildInfo(build)
    build.arbIndex?.let { ArbWarning(it) }
    Actions(build = build, fetchingLink = state.fetchingLink, fetchDownloadUrl = fetchDownloadUrl)
}

@Composable
private fun BuildInfo(build: CnRomBuild) {
    val context = LocalContext.current
    GroupedSection(Modifier.widthIn(max = 640.dp)) {
        build.securityPatch?.let { GroupedRow(title = stringResource(R.string.cn_roms_security_patch), value = it) }
        build.sizeBytes?.let {
            GroupedRow(title = stringResource(R.string.cn_roms_size), value = Formatter.formatFileSize(context, it))
        }
        build.published?.let { GroupedRow(title = stringResource(R.string.cn_roms_published), value = it) }
        build.otaVersion?.let { SelectableRow(stringResource(R.string.cn_roms_ota_version), it) }
        build.md5?.let { SelectableRow(stringResource(R.string.cn_roms_md5), it) }
    }
}

/** Long values (hashes, version strings) go below the label, and can be selected & copied */
@Composable
private fun SelectableRow(title: String, value: String) = Column(
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
) {
    Text(title, fontSize = 17.sp)
    SelectionContainer {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ArbWarning(index: Int) = Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        .widthIn(max = 640.dp)
        .fillMaxWidth()
        .clip(RoundedRectangle(20.dp))
        .background(groupedCardColor)
        .padding(16.dp)
        .testTag(CnRomsScreen_ArbWarningTestTag)
) {
    val red = Color(0xFFFF3B30)
    Icon(Symbols.Error, null, tint = red)
    Column {
        Text(stringResource(R.string.cn_roms_arb_title, index), color = red, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = stringResource(R.string.cn_roms_arb_text),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun Actions(
    build: CnRomBuild,
    fetchingLink: Boolean,
    fetchDownloadUrl: (onResult: (String?) -> Unit) -> Unit,
) = Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
        .padding(start = 16.dp, end = 16.dp, top = 20.dp)
        .widthIn(max = 640.dp)
        .fillMaxWidth()
) {
    val context = LocalContext.current
    CapsuleButton(
        text = stringResource(R.string.cn_roms_download),
        icon = Symbols.Download,
        filled = true,
        enabled = !fetchingLink,
        onClick = {
            fetchDownloadUrl { url ->
                if (url == null) context.showToast(context.getString(R.string.cn_roms_link_failed))
                else if (context.enqueueDownload(url, build)) {
                    context.showToast(context.getString(R.string.cn_roms_download_started))
                } else context.openLink(url) // no DownloadManager: let the browser handle it
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CnRomsScreen_DownloadButtonTestTag)
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CapsuleButton(
            text = stringResource(R.string.cn_roms_copy_link),
            icon = Symbols.Link,
            enabled = !fetchingLink,
            onClick = {
                fetchDownloadUrl { url ->
                    if (url == null) context.showToast(context.getString(R.string.cn_roms_link_failed)) else {
                        context.copyToClipboard(url)
                        context.showToast(context.getString(R.string.cn_roms_link_copied))
                    }
                }
            },
            modifier = Modifier.weight(1f)
        )

        build.changelogUrl?.let { url ->
            val customTabIntent = rememberCustomTabsIntent()
            CapsuleButton(
                text = stringResource(R.string.cn_roms_release_notes),
                icon = Symbols.Notes,
                onClick = { customTabIntent.launch(context, url) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (fetchingLink) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}

/**
 * Hands the (multi-GB) download to the system's DownloadManager, which survives the app being closed,
 * and saves to the public Downloads folder.
 *
 * @return `false` if DownloadManager is unavailable or refused the request
 */
private fun Context.enqueueDownload(url: String, build: CnRomBuild) = try {
    val request = DownloadManager.Request(url.toUri())
        .setTitle(build.fileName)
        .setDescription(build.deviceName.ifEmpty { build.label })
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, build.fileName)
        .setMimeType("application/zip")
    getSystemService<DownloadManager>()?.enqueue(request) != null
} catch (_: Exception) {
    false
}

@VisibleForTesting
val CnRomDeviceSearchFilter: (CnRomDevice, String) -> Boolean = { device, query ->
    device.name.contains(query, ignoreCase = true)
            || device.brand.contains(query, ignoreCase = true)
            || device.models.any { it.contains(query, ignoreCase = true) }
}

private const val TAG = "CnRomsScreen"

@VisibleForTesting
const val CnRomsScreen_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
const val CnRomsScreen_DeviceRowTestTag = TAG + "_DeviceRow"

@VisibleForTesting
const val CnRomsScreen_BuildRowTestTag = TAG + "_BuildRow"

@VisibleForTesting
const val CnRomsScreen_DetailSheetTestTag = TAG + "_DetailSheet"

@VisibleForTesting
const val CnRomsScreen_ArbWarningTestTag = TAG + "_ArbWarning"

@VisibleForTesting
const val CnRomsScreen_DownloadButtonTestTag = TAG + "_DownloadButton"

@VisibleForTesting
val PreviewCnRomDevices = listOf(
    CnRomDevice(
        key = "OP 13",
        name = "OnePlus 13",
        brand = "OnePlus",
        builds = listOf("PJZ110_16.0.3.500(CN01)", "PJZ110_16.0.2.400(CN01)", "PJZ110_15.0.0.862(CN01)"),
    ),
)

@PreviewThemes
@Composable
fun PreviewCnRomsScreen() = PreviewAppTheme {
    CnRomsScreen(
        navType = NavType.BottomBar,
        state = CnRomsState(
            refreshing = false,
            devices = PreviewCnRomDevices,
            thisDeviceKey = "OP 13",
            selectedKey = "OP 13",
        ),
        onRefresh = {},
        onSelectDevice = {},
        onOpenBuild = {},
    )
}
