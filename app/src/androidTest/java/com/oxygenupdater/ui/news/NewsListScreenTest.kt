package com.oxygenupdater.ui.news

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.R
import com.oxygenupdater.assertAndPerformClick
import com.oxygenupdater.assertSizeIsEqualTo
import com.oxygenupdater.get
import com.oxygenupdater.models.Article
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.ErrorStateTestTag
import com.oxygenupdater.ui.common.ErrorState_TextTestTag
import com.oxygenupdater.ui.common.ErrorState_TitleTestTag
import com.oxygenupdater.ui.common.IconTextButtonTestTag
import com.oxygenupdater.ui.main.NavType
import org.junit.Test

class NewsListScreenTest : ComposeBaseTest() {

    private val itemCount = PreviewNewsListData.size

    @Test
    fun newsListScreen_errorAllRead() {
        setContent(
            navType = NavType.BottomBar,
            windowWidthSize = WindowWidthSizeClass.Compact,
            windowHeightSize = WindowHeightSizeClass.Expanded,
            data = PreviewNewsListData.filter { it.readState },
        )

        // Segmented control — switch to "Unread"
        filterSegments()[1].assertAndPerformClick()

        rule[ErrorState_TitleTestTag].assertHasTextExactly(R.string.news_empty_state_all_read_header)
        rule[ErrorState_TextTestTag].assertHasTextExactly(R.string.news_empty_state_all_read_text)

        rule[IconTextButtonTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyColumnTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyVerticalGridTestTag].assertDoesNotExist()
    }

    @Test
    fun newsListScreen_errorEmpty() {
        setContent(
            navType = NavType.BottomBar,
            windowWidthSize = WindowWidthSizeClass.Compact,
            windowHeightSize = WindowHeightSizeClass.Expanded,
            data = listOf(),
        )

        rule[ErrorState_TitleTestTag].assertHasTextExactly(R.string.news_empty_state_none_available_header)
        rule[ErrorState_TextTestTag].assertHasTextExactly(R.string.news_empty_state_none_available_text)

        rule[IconTextButtonTestTag].assertAndPerformClick()
        ensureCallbackInvokedExactlyOnce("onRefresh")

        rule[NewsListScreen_LazyColumnTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyVerticalGridTestTag].assertDoesNotExist()
    }

    @Test
    fun newsListScreen_bottomBar() {
        setContent(
            navType = NavType.BottomBar,
            windowWidthSize = WindowWidthSizeClass.Compact,
            windowHeightSize = WindowHeightSizeClass.Expanded,
        )

        rule[NewsListScreen_LazyVerticalGridTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyColumnTestTag].assertExists()
        validateItems(imageSize = 84.dp)
    }

    @Test
    fun newsListScreen_sideRail() {
        setContent(
            navType = NavType.SideRail,
            windowWidthSize = WindowWidthSizeClass.Expanded,
            windowHeightSize = WindowHeightSizeClass.Expanded,
        )

        rule[NewsListScreen_LazyColumnTestTag].assertDoesNotExist()
        rule[NewsListScreen_LazyVerticalGridTestTag].assertExists()
        validateItems(imageSize = null) // image fills the card's width
    }

    private fun filterSegments() = rule.onAllNodes(
        hasAnyAncestor(hasTestTag(NewsListScreen_FilterTestTag)) and hasClickAction()
    ).assertCountEquals(2)

    private fun validateItems(imageSize: Dp?) {
        rule[ErrorStateTestTag].assertDoesNotExist()
        filterSegments()

        rule.onAllNodesWithTag(NewsListScreen_ItemColumnTestTag).assertCountEquals(itemCount)
        rule.onAllNodesWithTag(NewsListScreen_ItemImageTestTag, true).run {
            assertCountEquals(itemCount)
            if (imageSize != null) repeat(itemCount) { get(it).assertSizeIsEqualTo(imageSize) }
        }

        // Unread articles have a dot
        val unread = PreviewNewsListData.count { !it.readState }
        rule.onAllNodesWithTag(NewsListScreen_ItemBadgeTestTag, true).run {
            assertCountEquals(unread)
            repeat(unread) { get(it).assertSizeIsEqualTo(8.dp) }
        }

        PreviewNewsListData.forEach(::validateItem)
    }

    private fun validateItem(article: Article) {
        val node = rule.onNode(hasTestTag(NewsListScreen_ItemColumnTestTag) and hasText(article.title!!))
        node.assert(hasText(article.getFooterText()))
        node.assert(hasText(article.subtitle!!))

        val menu = rule[NewsListScreen_ItemMenuTestTag]

        node.performTouchInput { longClick() } // should open context menu now
        menu.onChildAt(0).assertAndPerformClick() // toggle read status
        ensureCallbackInvokedExactlyOnce("onToggleReadClick: ${article.id}")
        menu.assertDoesNotExist() // menu should be hidden now

        node.performTouchInput { longClick() } // re-open menu
        menu.onChildAt(3).assertAndPerformClick() // copy link
        assertCopiedToClipboard(article.webUrl)
        menu.assertDoesNotExist()

        // Finally, test tapping on the item
        node.assertAndPerformClick()
        ensureCallbackInvokedExactlyOnce("openItem: ${article.id}")
    }

    private fun setContent(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
        windowHeightSize: WindowHeightSizeClass,
        data: List<Article> = PreviewNewsListData,
    ) = setContent {
        NewsListScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
            windowHeightSize = windowHeightSize,
            state = RefreshAwareState(false, data),
            onRefresh = { trackCallback("onRefresh") },
            unreadCountState = remember { mutableIntStateOf(data.count { !it.readState }) },
            onMarkAllReadClick = { trackCallback("onMarkAllReadClick") },
            onToggleReadClick = { trackCallback("onToggleReadClick: ${it.id}") },
            openItem = { trackCallback("openItem: $it") },
        )
    }
}
