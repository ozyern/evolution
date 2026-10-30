package com.oxygenupdater.ui.news

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygenupdater.extensions.copyToClipboard
import com.oxygenupdater.extensions.shareExternally
import com.oxygenupdater.icons.Link
import com.oxygenupdater.icons.Share
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.models.Article
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.CapsuleButton
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.modifierDefaultPaddingTop
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.common.scrollbar.Scrollbar
import com.oxygenupdater.ui.common.scrollbar.scrollbarState
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.currentLocale
import com.oxygenupdater.ui.dialogs.ArticleErrorSheet
import com.oxygenupdater.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes
import com.oxygenupdater.ui.theme.light
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    viewModel: ArticleViewModel,
    id: Long,
    scrollBehavior: TopAppBarScrollBehavior,
    onLoadFinished: (Article) -> Unit,
) {
    DisposableEffect(Unit) {
        viewModel.refreshItem(id)
        // Clear to avoid loading the previous article in the WebView
        onDispose(viewModel::clearItem)
    }

    val webViewState = rememberWebViewState()
    val onRefresh: () -> Unit = {
        viewModel.refreshItem(id)
        webViewState.webView?.reload()
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    ArticleScreen(
        state = state,
        onRefresh = onRefresh,
        webViewState = webViewState,
        onLoadFinished = onLoadFinished,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    )
}

@VisibleForTesting
@Composable
fun ArticleScreen(
    modifier: Modifier,
    state: RefreshAwareState<Article?>,
    onRefresh: () -> Unit,
    webViewState: WebViewState,
    onLoadFinished: (Article) -> Unit,
) {
    val (refreshing, data) = state
    // We're using `id` not just for efficiency, but also because of https://kotlinlang.slack.com/archives/CJLTWPH7S/p1693203706074269.
    // The `key` overload was deprecated in Compose 1.9.0-alpha02.
    val item = rememberSaveable(data?.id ?: return) { data }

    PullRefresh(
        state = state,
        shouldShowProgressIndicator = { it?.isFullyLoaded != true },
        onRefresh = onRefresh,
    ) {
        var errorTitle by rememberState<String?>(null)
        errorTitle?.let { title ->
            ModalBottomSheet({ errorTitle = null }) { ArticleErrorSheet(it, title, confirm = onRefresh) }
        }

        val scrollState = rememberScrollState()
        Column(
            modifier
                .fillMaxHeight()
                .verticalScroll(scrollState)
        ) {
            // Like Apple News: a "deck" (summary) under the headline, a dateline, then actions
            val colorScheme = MaterialTheme.colorScheme
            val textModifier = Modifier.withPlaceholder(refreshing)
            var showDivider = false

            item.subtitle?.let {
                showDivider = true
                Text(
                    text = it,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 19.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                        .then(textModifier)
                        .testTag(ArticleScreen_SubtitleTestTag)
                )
            }

            item.getRelativeTime()?.let {
                showDivider = true
                Text(
                    text = it,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                        .then(textModifier)
                        .testTag(ArticleScreen_TimeTestTag)
                )
            }

            Buttons(item = item)

            // We can't edit CSS in WebViews, so pass theme to backend to style it accordingly
            val language = if (currentLocale().language == "nl") "NL" else "EN"
            val theme = if (MaterialTheme.colorScheme.light) "Light" else "Dark"
            val url = "${item.apiUrl}/$language/$theme?ads=false"
            RefreshAwareWebView(
                refreshing = refreshing,
                webViewState = webViewState,
                url = url,
                showDivider = showDivider,
                onError = { errorTitle = it },
                onLoadFinished = { onLoadFinished(item) },
            )
        }

        // Must be at the end — highest in the Z-hierarchy => nothing draws above it
        // TODO(compose/news): switch to first-party solution when it's out: https://developer.android.com/jetpack/androidx/compose-roadmap
        scrollState.Scrollbar(
            state = scrollState.scrollbarState(),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@SuppressLint("PrivateResource")
@Composable
private fun Buttons(item: Article) = Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)
) {
    val context = LocalContext.current
    CapsuleButton(
        text = stringResource(androidx.browser.R.string.fallback_menu_item_share_link),
        icon = Symbols.Share,
        onClick = { context.shareExternally(item.title ?: "", item.webUrl) },
        modifier = Modifier
            .weight(1f)
            .testTag(ArticleScreen_ShareButtonTestTag)
    )
    CapsuleButton(
        text = stringResource(androidx.browser.R.string.fallback_menu_item_copy_link),
        icon = Symbols.Link,
        onClick = { context.copyToClipboard(item.webUrl) },
        modifier = Modifier
            .weight(1f)
            .testTag(ArticleScreen_CopyButtonTestTag)
    )
}

private const val TAG = "ArticleScreen"

@VisibleForTesting
const val ArticleScreen_SubtitleTestTag = TAG + "_Subtitle"

@VisibleForTesting
const val ArticleScreen_TimeTestTag = TAG + "_Time"

@VisibleForTesting
const val ArticleScreen_ShareButtonTestTag = TAG + "_ShareButton"

@VisibleForTesting
const val ArticleScreen_CopyButtonTestTag = TAG + "_CopyButton"

@Composable
private fun RefreshAwareWebView(
    refreshing: Boolean,
    webViewState: WebViewState,
    url: String,
    showDivider: Boolean,
    onError: (String) -> Unit,
    onLoadFinished: () -> Unit,
) {
    LaunchedEffect(url) { webViewState.webView?.loadUrl(url) }

    val loading = webViewState.loadingState
    val paddingTop = if (showDivider) Modifier.padding(top = 13.dp) else Modifier
    if (refreshing || loading == LoadingState.Initializing) LinearProgressIndicator(
        paddingTop then modifierMaxWidth
    ) else if (loading is LoadingState.Loading) LinearProgressIndicator(
        progress = { loading.progress },
        modifier = paddingTop then modifierMaxWidth
    ) else if (loading == LoadingState.Finished) {
        /** Errors not for our URLs are filtered out in [WebViewClient] */
        val error = webViewState.errorForCurrentRequest?.errorCodeString

        // Ensure finished & error callbacks are done just once per load
        LaunchedEffect(error) {
            if (error == null) onLoadFinished() else onError(error)
        }

        if (showDivider) HorizontalDivider(modifierDefaultPaddingTop)
    }

    WebView(
        state = webViewState,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 8.dp) // news-content HTML already has an 8px margin
    )
}

@VisibleForTesting
fun Article.getRelativeTime() = epochMilli?.let {
    // Weird bug in `android.text.format` that causes a crash in
    // older Android versions, due to using the [FORMAT_SHOW_TIME]
    // flag. Hacky workaround is to call the function again, but
    // without this flag. See https://stackoverflow.com/a/52665211.
    try {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    } catch (_: NullPointerException) {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        )
    }
}?.toString()

@VisibleForTesting
val PreviewArticleData = LocalDateTime.now().let { now ->
    Article(
        id = 1,
        title = "An unnecessarily long news title, to get an accurate understanding of how long titles are rendered",
        subtitle = "An unnecessarily long news subtitle, to get an accurate understanding of how long subtitles are rendered",
        imageUrl = "https://github.com/oxygen-updater.png",
        text = "Text",
        datePublished = now.minusHours(2).toString(),
        dateLastEdited = now.toString(),
        authorName = "Author",
        read = false,
    )
}

@PreviewThemes
@Composable
fun PreviewArticleScreen() = PreviewAppTheme {
    ArticleScreen(
        state = RefreshAwareState(false, PreviewArticleData),
        onRefresh = {},
        webViewState = rememberWebViewState(),
        onLoadFinished = {},
        modifier = Modifier,
    )
}
