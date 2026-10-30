package com.ozyern.evolution.ui.news

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kyant.shapes.RoundedRectangle
import com.ozyern.evolution.R
import com.ozyern.evolution.extensions.copyToClipboard
import com.ozyern.evolution.extensions.launch
import com.ozyern.evolution.extensions.rememberCustomTabsIntent
import com.ozyern.evolution.extensions.shareExternally
import com.ozyern.evolution.icons.Cancel
import com.ozyern.evolution.icons.CheckCircle
import com.ozyern.evolution.icons.FullCoverage
import com.ozyern.evolution.icons.ImageFilled
import com.ozyern.evolution.icons.Link
import com.ozyern.evolution.icons.OpenInBrowser
import com.ozyern.evolution.icons.Share
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.internal.NotSetL
import com.ozyern.evolution.models.Article
import com.ozyern.evolution.ui.RefreshAwareState
import com.ozyern.evolution.ui.common.ConditionalNavBarPadding
import com.ozyern.evolution.ui.common.DropdownMenuItem
import com.ozyern.evolution.ui.common.ErrorState
import com.ozyern.evolution.ui.common.GroupedCardShape
import com.ozyern.evolution.ui.common.PullRefresh
import com.ozyern.evolution.ui.common.SegmentedControl
import com.ozyern.evolution.ui.common.groupedBackground
import com.ozyern.evolution.ui.common.groupedCardColor
import com.ozyern.evolution.ui.common.rememberState
import com.ozyern.evolution.ui.common.scrollbar.Scrollbar
import com.ozyern.evolution.ui.common.scrollbar.rememberDraggableScroller
import com.ozyern.evolution.ui.common.scrollbar.scrollbarState
import com.ozyern.evolution.ui.common.withPlaceholder
import com.ozyern.evolution.ui.main.LiquidDockReservedHeight
import com.ozyern.evolution.ui.main.MainRoute
import com.ozyern.evolution.ui.main.NavType
import com.ozyern.evolution.ui.theme.PreviewAppTheme
import com.ozyern.evolution.ui.theme.PreviewThemes
import com.ozyern.evolution.ui.theme.PreviewWindowSize
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Styled after Apple News: a segmented "All / Unread" filter, then each article as a rounded card with
 * a meta line (unread dot, time & author), a bold headline, a short summary and a thumbnail. Long-press
 * an article for its context menu.
 */
@Composable
fun NewsListScreen(
    navType: NavType,
    windowWidthSize: WindowWidthSizeClass,
    windowHeightSize: WindowHeightSizeClass,
    state: RefreshAwareState<List<Article>>,
    onRefresh: () -> Unit,
    unreadCountState: MutableIntState,
    onMarkAllReadClick: () -> Unit,
    onToggleReadClick: (Article) -> Unit,
    openItem: (id: Long) -> Unit,
) = Box(
    Modifier
        .fillMaxSize()
        .background(groupedBackground)
) {
    PullRefresh(
        state = state,
        shouldShowProgressIndicator = { it.isEmpty() },
        onRefresh = onRefresh,
    ) {
        val refreshing = state.refreshing
        var onlyUnread by rememberState(false)

        val data = if (onlyUnread) state.data.filterNot { it.readState } else state.data
        val list = if (!refreshing) rememberSaveable(data) { data } else data

        LaunchedEffect(onlyUnread) {
            unreadCountState.intValue = if (onlyUnread) list.size else list.count { !it.readState }
        }

        val unreadCount by remember { unreadCountState }
        MainRoute.NewsList.badge = unreadCount.let { if (it == 0) null else it.toString() }

        val filter: @Composable () -> Unit = {
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.news_filter_all),
                    stringResource(R.string.news_filter_unread, unreadCount),
                ),
                selectedIndex = if (onlyUnread) 1 else 0,
                onSelect = { onlyUnread = it == 1 },
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                    .widthIn(max = 480.dp)
                    .testTag(NewsListScreen_FilterTestTag)
            )
        }

        if (onlyUnread && unreadCount == 0) Column {
            filter()
            ErrorState(
                navType = navType,
                titleResId = R.string.news_empty_state_all_read_header,
                icon = Symbols.FullCoverage,
                textResId = R.string.news_empty_state_all_read_text,
                rich = false,
                onRefreshClick = null,
            )
        } else if (list.isEmpty()) ErrorState(
            navType = navType,
            titleResId = R.string.news_empty_state_none_available_header,
            icon = Symbols.FullCoverage,
            textResId = R.string.news_empty_state_none_available_text,
            rich = false,
            onRefreshClick = onRefresh,
        ) else {
            val onItemToggleRead: (Article) -> Unit = {
                onToggleReadClick(it)
                if (it.readState) unreadCountState.intValue++ else {
                    // Coerce to at least 0, just in case there's an inconsistency
                    if (unreadCountState.intValue > 0) unreadCountState.intValue--
                }
                it.readState = !it.readState
            }
            val onItemClick: (Article) -> Unit = {
                ArticleViewModel.item = it
                openItem(it.id ?: NotSetL)
            }

            if (navType == NavType.BottomBar) {
                val listState = rememberLazyListState()

                // Run when we have new articles, by keying the first item's ID.
                // This should be more performant than using the list itself as the key.
                LaunchedEffect(list.firstOrNull()?.id) {
                    // Scroll to the first item upon refresh
                    if (listState.firstVisibleItemIndex > 0) try {
                        listState.animateScrollToItem(0)
                    } catch (_: Exception) {
                        try {
                            listState.scrollToItem(0)
                        } catch (_: Exception) {
                            // ignore
                        }
                    }
                }

                // Let items scroll under the floating glass dock, but keep the last one reachable
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = navBarBottom + LiquidDockReservedHeight),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(NewsListScreen_LazyColumnTestTag)
                ) {
                    item(key = "filter") { filter() }

                    items(items = list, key = { it.id ?: Random.nextLong() }) {
                        NewsCard(
                            refreshing = refreshing,
                            item = it,
                            imageOnTop = false,
                            onToggleReadClick = { onItemToggleRead(it) },
                            onClick = { onItemClick(it) },
                            modifier = Modifier
                                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                // Must be at the end — highest in the Z-hierarchy => nothing draws above it,
                // and user can press/drag for fast scrolling.
                val numItems = list.size + 1 // + filter
                listState.Scrollbar(
                    state = listState.scrollbarState(numItems),
                    onThumbMoved = listState.rememberDraggableScroller(numItems),
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            } else {
                val gridState = rememberLazyGridState()
                val minCardWidth = when (windowWidthSize) {
                    WindowWidthSizeClass.Expanded -> 320.dp
                    else -> 280.dp
                }

                // Run when we have new articles, by keying the first item's ID.
                // This should be more performant than using the list itself as the key.
                LaunchedEffect(list.firstOrNull()?.id) {
                    // Scroll to the first item upon refresh
                    if (gridState.firstVisibleItemIndex > 0) try {
                        gridState.animateScrollToItem(0)
                    } catch (_: Exception) {
                        try {
                            gridState.scrollToItem(0)
                        } catch (_: Exception) {
                            // ignore
                        }
                    }
                }

                Column {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minCardWidth),
                        state = gridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            // Leave space for 2/3-button nav bar in landscape mode
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .testTag(NewsListScreen_LazyVerticalGridTestTag)
                    ) {
                        item(key = "filter", span = { GridItemSpan(maxLineSpan) }) {
                            Box(contentAlignment = Alignment.Center) { filter() }
                        }

                        items(items = list, key = { it.id ?: Random.nextLong() }) {
                            NewsCard(
                                refreshing = refreshing,
                                item = it,
                                imageOnTop = windowHeightSize != WindowHeightSizeClass.Compact,
                                onToggleReadClick = { onItemToggleRead(it) },
                                onClick = { onItemClick(it) },
                                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                            )
                        }
                    }

                    ConditionalNavBarPadding(navType)
                }
            }
        }
    }
}

/**
 * @param imageOnTop `true` for a tall card with a wide image above the text (grids on larger screens),
 *   `false` for a compact card with a square thumbnail at the trailing edge (phones)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NewsCard(
    refreshing: Boolean,
    item: Article,
    imageOnTop: Boolean,
    onToggleReadClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(modifier) {
    var showMenu by rememberState(false)
    val haptic = LocalHapticFeedback.current
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(GroupedCardShape)
        .background(groupedCardColor)
        .combinedClickable(
            enabled = !refreshing,
            onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showMenu = true
            },
            onClick = onClick,
        )
        .testTag(NewsListScreen_ItemColumnTestTag)

    // Read articles recede, like read mail
    val readAlpha = Modifier.graphicsLayer {
        if (!refreshing && item.readState) alpha = 0.6f
    }

    if (imageOnTop) Column(cardModifier) {
        NewsImage(
            refreshing = refreshing,
            item = item,
            size = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .then(readAlpha)
        )
        Column(Modifier.padding(14.dp)) {
            CardText(refreshing = refreshing, item = item, readAlpha = readAlpha)
        }
    } else Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = cardModifier.padding(14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            CardText(refreshing = refreshing, item = item, readAlpha = readAlpha)
        }
        NewsImage(
            refreshing = refreshing,
            item = item,
            size = 84.dp,
            modifier = Modifier
                .clip(RoundedRectangle(12.dp))
                .then(readAlpha)
        )
    }

    ItemMenu(showMenu, { showMenu = false }, item, onToggleReadClick)
}

@Composable
private fun CardText(refreshing: Boolean, item: Article, readAlpha: Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    // Meta line: unread dot (like Mail), relative time & author
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!refreshing && !item.readState) Box(
            Modifier
                .padding(end = 6.dp)
                .size(8.dp)
                .clip(RoundedRectangle(4.dp))
                .background(colorScheme.primary)
                .testTag(NewsListScreen_ItemBadgeTestTag)
        )

        Text(
            text = item.getFooterText(),
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.withPlaceholder(refreshing)
        )
    }

    Text(
        text = item.title ?: "Unknown title",
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(top = 4.dp)
            .withPlaceholder(refreshing)
            .then(readAlpha)
    )

    item.subtitle?.takeIf { it.isNotBlank() }?.let {
        Text(
            text = it,
            color = colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .withPlaceholder(refreshing)
        )
    }
}

/** @param size fixed square size, or `null` to fill the space given by [modifier] */
@Composable
private fun NewsImage(
    modifier: Modifier = Modifier,
    refreshing: Boolean,
    item: Article,
    size: Dp?,
) {
    val defaultImage = Symbols.ImageFilled.run {
        rememberVectorPainter(
            defaultWidth = defaultWidth,
            defaultHeight = defaultHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            name = name,
            tintColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tintBlendMode = tintBlendMode,
            autoMirror = autoMirror,
            content = { _, _ -> RenderVectorGroup(group = root) }
        )
    }

    AsyncImage(
        model = item.imageUrl?.let {
            val context = LocalContext.current
            val sizePx = size?.let { size -> LocalDensity.current.run { size.roundToPx() } }
            remember(it, sizePx) {
                ImageRequest.Builder(context)
                    .data(it)
                    .apply { if (sizePx != null) size(sizePx) }
                    .build()
            }
        },
        contentDescription = stringResource(R.string.icon),
        placeholder = defaultImage,
        error = defaultImage,
        contentScale = ContentScale.Crop,
        modifier = (if (size != null) Modifier.size(size) else Modifier)
            .then(modifier)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .withPlaceholder(refreshing)
            .testTag(NewsListScreen_ItemImageTestTag)
    )
}

/** Long-press context menu */
@SuppressLint("PrivateResource")
@Composable
private fun ItemMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    item: Article,
    onToggleReadClick: () -> Unit,
) = DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismiss,
    shape = RoundedRectangle(14.dp),
    modifier = Modifier.testTag(NewsListScreen_ItemMenuTestTag)
) {
    DropdownMenuItem(
        icon = if (item.readState) Symbols.Cancel else Symbols.CheckCircle,
        textResId = if (item.readState) R.string.news_mark_unread else R.string.news_mark_read,
        onClick = {
            onToggleReadClick()
            onDismiss()
        },
    )

    val context = LocalContext.current
    val customTabIntent = rememberCustomTabsIntent()
    DropdownMenuItem(
        icon = Symbols.OpenInBrowser,
        textResId = androidx.browser.R.string.fallback_menu_item_open_in_browser,
        onClick = {
            customTabIntent.launch(context, item.webUrl)
            onDismiss()
        },
    )

    DropdownMenuItem(
        icon = Symbols.Share,
        textResId = androidx.browser.R.string.fallback_menu_item_share_link,
        onClick = {
            context.shareExternally(item.title ?: "", item.webUrl)
            onDismiss()
        },
    )

    DropdownMenuItem(
        icon = Symbols.Link,
        textResId = androidx.browser.R.string.fallback_menu_item_copy_link,
        onClick = {
            context.copyToClipboard(item.webUrl)
            onDismiss()
        },
    )
}

@VisibleForTesting
fun Article.getFooterText(): String {
    val authorName = authorName ?: "Unknown Author"
    return epochMilli?.let {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_ALL
        )
    }?.let {
        "$it • $authorName"
    } ?: authorName
}

private const val TAG = "NewsListScreen"

@VisibleForTesting
const val NewsListScreen_FilterTestTag = TAG + "_Filter"

@VisibleForTesting
const val NewsListScreen_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
const val NewsListScreen_LazyVerticalGridTestTag = TAG + "_LazyVerticalGrid"

@VisibleForTesting
const val NewsListScreen_ItemColumnTestTag = TAG + "_ItemColumn"

@VisibleForTesting
const val NewsListScreen_ItemBadgeTestTag = TAG + "_ItemBadge"

@VisibleForTesting
const val NewsListScreen_ItemImageTestTag = TAG + "_ItemImage"

@VisibleForTesting
const val NewsListScreen_ItemMenuTestTag = TAG + "_ItemMenu"

@VisibleForTesting
val PreviewNewsListData = LocalDateTime.now().let { now ->
    val subtitle = "Unnecessarily long subtitle, to get an accurate understanding of how its rendered"
    listOf(
        Article(
            id = 1,
            title = "Oxygen Updater",
            subtitle = subtitle,
            imageUrl = "https://github.com/oxygen-updater.png",
            text = "",
            datePublished = now.minusDays(1).toString(),
            dateLastEdited = now.minusHours(4).toString(),
            authorName = "Author",
            read = true,
        ),
        Article(
            id = 2,
            title = "An unnecessarily long article title, to get an accurate understanding of how long titles are rendered. This line has a length of 255 characters and should provide enough information to tweak the AppBar UI for the best balance between readability & design.",
            subtitle = subtitle,
            imageUrl = "https://github.com/oxygen-updater.png",
            text = "",
            datePublished = now.minusDays(2).toString(),
            dateLastEdited = now.minusHours(5).toString(),
            authorName = "Author",
            read = false,
        ),
    )
}

@PreviewThemes
@Composable
fun PreviewNewsListScreen() = PreviewAppTheme {
    val windowSize = PreviewWindowSize
    NewsListScreen(
        navType = NavType.from(windowSize.widthSizeClass),
        windowWidthSize = windowSize.widthSizeClass,
        windowHeightSize = windowSize.heightSizeClass,
        state = RefreshAwareState(false, PreviewNewsListData),
        onRefresh = {},
        unreadCountState = rememberState(1),
        onMarkAllReadClick = {},
        onToggleReadClick = {},
        openItem = {},
    )
}
