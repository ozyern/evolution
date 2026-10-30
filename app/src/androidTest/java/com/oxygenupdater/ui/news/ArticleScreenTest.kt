package com.oxygenupdater.ui.news

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.assertIsNotFocused
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.get
import com.oxygenupdater.ui.RefreshAwareState
import org.junit.Test

class ArticleScreenTest : ComposeBaseTest() {

    @Test
    fun articleScreen() {
        val webViewState = WebViewState()
        setContent {
            ArticleScreen(
                state = RefreshAwareState(false, PreviewArticleData),
                onRefresh = {},
                webViewState = webViewState,
                onLoadFinished = { trackCallback("onLoadFinished: ${it.id}") },
                modifier = Modifier
            )
        }

        rule[WebViewTestTag].assertExists()

        val webViewIdlingResource = object : IdlingResource {
            override val isIdleNow: Boolean
                get() = webViewState.loadingState == LoadingState.Finished

            override fun getDiagnosticMessageIfBusy() = "WebView hasn't finished loading yet"
        }

        rule.registerIdlingResource(webViewIdlingResource)
        rule.runOnIdle {
            ensureCallbackInvokedExactlyOnce("onLoadFinished: ${PreviewArticleData.id}")
        }
        rule.unregisterIdlingResource(webViewIdlingResource)

        rule[ArticleScreen_TimeTestTag].assertHasTextExactly(PreviewArticleData.getRelativeTime())
        rule[ArticleScreen_SubtitleTestTag].assertHasTextExactly(PreviewArticleData.subtitle)
        rule[ArticleScreen_CopyButtonTestTag].run {
            assertHasTextExactly(androidx.browser.R.string.fallback_menu_item_copy_link)
            assertAndPerformClick()
            assertCopiedToClipboard(PreviewArticleData.webUrl)
        }

        // Tested last, because clicking it leaves the app, preventing further testing
        rule[ArticleScreen_ShareButtonTestTag].run {
            assertHasTextExactly(androidx.browser.R.string.fallback_menu_item_share_link)
            assertAndPerformClick() // should open share sheet
            try {
                assertIsNotFocused()
            } catch (e: IllegalStateException) {
                // This is likely "No compose hierarchies found in the app", which is
                // fine because that's what we expect (we're in the share sheet now).
            }
        }
    }
}
