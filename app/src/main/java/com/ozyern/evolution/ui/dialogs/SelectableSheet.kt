package com.ozyern.evolution.ui.dialogs

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.ozyern.evolution.R
import com.ozyern.evolution.icons.Check
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.icons.WandStars
import com.ozyern.evolution.internal.NotSet
import com.ozyern.evolution.internal.NotSetL
import com.ozyern.evolution.models.SelectableModel
import com.ozyern.evolution.ui.SettingsListConfig
import com.ozyern.evolution.ui.common.GlassSearchBar
import com.ozyern.evolution.ui.common.GlassSearchBarReservedHeight
import com.ozyern.evolution.ui.common.GroupedFooter
import com.ozyern.evolution.ui.common.animatedClickable
import com.ozyern.evolution.ui.common.groupedBackground
import com.ozyern.evolution.ui.common.groupedCell
import com.ozyern.evolution.ui.common.modifierSemanticsNotSelected
import com.ozyern.evolution.ui.common.modifierSemanticsSelected
import com.ozyern.evolution.ui.common.rememberState
import com.ozyern.evolution.ui.settings.DeviceSearchFilter
import com.ozyern.evolution.ui.settings.DeviceSettingsListConfig
import com.ozyern.evolution.ui.settings.MethodSettingsListConfig
import com.ozyern.evolution.ui.theme.PreviewThemes
import kotlinx.coroutines.delay

/**
 * iOS-style picker sheet: a centred title, options in an inset grouped card with a trailing checkmark
 * on the selected one, and an explanatory footer. If [filter] is set, the sheet opens full-height with a
 * Liquid Glass search field floating at the bottom (like iOS 26), over the list.
 */
@Composable
fun <T : SelectableModel> ColumnScope.SelectableSheet(
    hide: () -> Unit,
    config: SettingsListConfig<T>,
    @StringRes titleResId: Int,
    @StringRes captionResId: Int,
    filter: ((T, String) -> Boolean)? = null,
    onClick: (item: T) -> Unit,
) {
    SheetHeader(titleResId)

    val (list, recommendedId, selectedId) = config
    if (list.isEmpty()) return

    val initialFirstVisibleItemIndex = if (selectedId == NotSetL) 0 else remember(list, selectedId) {
        list.indexOfFirst { it.id == selectedId }.let { if (it == NotSet) 0 else it }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex)
    val caption = stringResource(captionResId)

    if (filter == null) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f, false)
                .testTag(SelectableSheet_LazyColumnTestTag)
        ) {
            options(list, recommendedId, selectedId, hide, onClick)
            footer(caption)
        }
        return
    }

    var query by rememberState("") // not saveable

    // Debounce search query changes to filter list
    var items by rememberState(list)
    LaunchedEffect(query) {
        items = if (query.isBlank()) list else {
            delay(100) // debounce by 100ms
            list.filter { filter(it, query.trim()) }
        }
    }

    // List is recorded into this backdrop so the floating search field can refract it
    val backdrop = rememberLayerBackdrop()
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = GlassSearchBarReservedHeight),
            modifier = Modifier
                .layerBackdrop(backdrop)
                .background(groupedBackground) // inside the recorded layer, so the glass has something to refract
                .testTag(SelectableSheet_LazyColumnTestTag)
        ) {
            if (items.isEmpty()) item(key = "empty") {
                Text(
                    text = stringResource(R.string.search_no_results, query.trim()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp)
                )
            } else options(items, recommendedId, selectedId, hide, onClick)
            footer(caption)
        }

        GlassSearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.search),
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
        )
    }
}

private fun <T : SelectableModel> LazyListScope.options(
    items: List<T>,
    recommendedId: Long,
    selectedId: Long,
    hide: () -> Unit,
    onClick: (item: T) -> Unit,
) {
    val lastIndex = items.lastIndex
    itemsIndexed(items = items, key = { _, it -> it.id }) { index, item ->
        val name = item.name ?: return@itemsIndexed
        val id = item.id
        val selected = selectedId == id
        val colorScheme = MaterialTheme.colorScheme

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .groupedCell(first = index == 0, last = index == lastIndex)
                .animatedClickable {
                    onClick(item)
                    hide()
                }
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp) // must be after `clickable`
                .testTag(BottomSheet_ItemRowTestTag)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                item.subtitle?.let {
                    Text(
                        text = it,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        overflow = TextOverflow.Ellipsis, maxLines = 1,
                    )
                }
            }

            if (id == recommendedId) Icon(
                imageVector = Symbols.WandStars,
                contentDescription = stringResource(R.string.theme_auto),
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            // Trailing checkmark, like iOS
            if (selected) Icon(
                imageVector = Symbols.Check,
                contentDescription = stringResource(R.string.summary_on),
                tint = colorScheme.primary,
                modifier = Modifier.size(22.dp) then modifierSemanticsSelected
            ) else Spacer(modifierSemanticsNotSelected.size(22.dp))
        }
    }
}

private fun LazyListScope.footer(caption: String) = item(key = "footer") {
    GroupedFooter(
        text = caption,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .testTag(BottomSheet_CaptionTestTag)
    )
}

private const val TAG = "SelectableSheet"

@VisibleForTesting
const val SelectableSheet_LazyColumnTestTag = TAG + "_LazyColumn"

@SuppressLint("VisibleForTests")
@PreviewThemes
@Composable
fun PreviewDeviceSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        config = DeviceSettingsListConfig,
        titleResId = R.string.settings_search_devices,
        captionResId = R.string.onboarding_device_chooser_caption,
        filter = DeviceSearchFilter,
        onClick = {},
    )
}

@SuppressLint("VisibleForTests")
@PreviewThemes
@Composable
fun PreviewMethodSheet() = PreviewModalBottomSheet {
    SelectableSheet(
        hide = {},
        config = MethodSettingsListConfig,
        titleResId = R.string.settings_update_method,
        captionResId = R.string.onboarding_method_chooser_caption,
        onClick = {},
    )
}
