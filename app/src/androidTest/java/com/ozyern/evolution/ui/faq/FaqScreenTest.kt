package com.ozyern.evolution.ui.faq

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import com.ozyern.evolution.ComposeBaseTest
import com.ozyern.evolution.get
import com.ozyern.evolution.models.InAppFaq
import com.ozyern.evolution.ui.RefreshAwareState
import com.ozyern.evolution.ui.common.RichText_ContainerTestTag
import com.ozyern.evolution.validateLazyColumn
import org.junit.Test

class FaqScreenTest : ComposeBaseTest() {

    @Test
    fun faqScreen_category() {
        val data = PreviewFaqScreenData.filter { it.type == TypeCategory }
        val itemCount = common(data)

        rule.onAllNodesWithTag(FaqScreen_ItemRowTestTag).assertCountEquals(0)
        rule.onAllNodesWithTag(RichText_ContainerTestTag).assertCountEquals(0)
        rule.onAllNodesWithTag(FaqScreen_CategoryTextTestTag).run {
            repeat(itemCount) { index ->
                get(index).assertHasTextExactly(data[index].title)
            }
        }
    }

    @Test
    fun faqScreen_item() {
        val data = PreviewFaqScreenData.filter { it.type == TypeItem }
        val itemCount = common(data)

        val nodes = rule.onAllNodesWithTag(FaqScreen_ItemRowTestTag).run {
            assertCountEquals(itemCount)
            assertAll(hasClickAction())
        }

        rule.onAllNodesWithTag(RichText_ContainerTestTag).run {
            /** Should not exist initially as it's wrapped in [com.ozyern.evolution.ui.common.ExpandCollapse] */
            assertCountEquals(0)
        }

        repeat(itemCount) { index ->
            nodes[index].run {
                performClick() // should show RichText now
                assertHasTextExactly(data[index].title)
            }
        }

        rule.onAllNodesWithTag(RichText_ContainerTestTag).assertCountEquals(itemCount)
    }

    private fun common(data: List<InAppFaq>): Int {
        setContent {
            FaqScreen(
                state = RefreshAwareState(false, data),
                onRefresh = {},
            )
        }

        val size = data.size
        rule[FaqScreen_LazyColumnTestTag].validateLazyColumn(size)
        return size
    }
}
