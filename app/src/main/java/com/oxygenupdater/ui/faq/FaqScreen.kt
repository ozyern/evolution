package com.oxygenupdater.ui.faq

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oxygenupdater.models.InAppFaq
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.common.Chevron
import com.oxygenupdater.ui.common.ExpandCollapse
import com.oxygenupdater.ui.common.GroupedHeader
import com.oxygenupdater.ui.common.PullRefresh
import com.oxygenupdater.ui.common.RichText
import com.oxygenupdater.ui.common.animatedClickable
import com.oxygenupdater.ui.common.groupedBackground
import com.oxygenupdater.ui.common.groupedCell
import com.oxygenupdater.ui.common.modifierMaxWidth
import com.oxygenupdater.ui.common.withPlaceholder
import com.oxygenupdater.ui.theme.PreviewAppTheme
import com.oxygenupdater.ui.theme.PreviewThemes

@Composable
fun FaqScreen(viewModel: FaqViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FaqScreen(state = state, onRefresh = viewModel::refresh)
}

@VisibleForTesting
@Composable
fun FaqScreen(
    state: RefreshAwareState<List<InAppFaq>>,
    onRefresh: () -> Unit,
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
        val (refreshing, data) = state
        val list = if (!refreshing) rememberSaveable(data) { data } else data
        val lastIndex = list.lastIndex

        val itemPlaceholderModifier = Modifier.withPlaceholder(refreshing)
        LazyColumn(
            Modifier
                .fillMaxSize()
                .testTag(FaqScreen_LazyColumnTestTag)
        ) {
            itemsIndexed(
                items = list,
                // Since the server flattens categories and items into a single JSON
                // array, we need to avoid `id` collisions. 10000 should be enough.
                key = { _, it ->
                    it.id + if (it.type == TypeCategory) 10000 else 0
                },
                contentType = { _, it -> it.type },
            ) { index, it ->
                // Categories become section headers; their items are grouped into one card
                if (it.type == TypeCategory) GroupedHeader(
                    text = it.title ?: "",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .then(itemPlaceholderModifier)
                        .testTag(FaqScreen_CategoryTextTestTag)
                ) else FaqItem(
                    item = it,
                    first = index == 0 || list[index - 1].type == TypeCategory,
                    last = index == lastIndex || list[index + 1].type == TypeCategory,
                    modifier = itemPlaceholderModifier
                )

                if (index == lastIndex) {
                    Spacer(Modifier.height(24.dp))
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

/** An expandable question, with an iOS disclosure chevron that rotates when open */
@VisibleForTesting
@Composable
fun FaqItem(
    item: InAppFaq,
    first: Boolean,
    last: Boolean,
    modifier: Modifier,
) = Column(Modifier.groupedCell(first = first, last = last)) {
    var expanded by remember { item.expanded }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "FaqChevronRotation")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifierMaxWidth
            .animatedClickable { expanded = !expanded }
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp) // must be after `clickable`
            .testTag(FaqScreen_ItemRowTestTag)
    ) {
        Text(
            text = item.title ?: "",
            fontSize = 17.sp,
            modifier = Modifier
                .weight(1f)
                .then(modifier)
        )
        Box(Modifier.rotate(rotation)) { Chevron() }
    }

    ExpandCollapse(expanded) {
        RichText(
            text = item.body,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                .then(modifier)
        )
    }
}

@VisibleForTesting
const val TypeCategory = "category"

@VisibleForTesting
const val TypeItem = "item"

private const val PreviewTitle = "An unnecessarily long FAQ entry, to get an accurate understanding of how long text is rendered"
private const val PreviewBodyHtml = """HTML markup, should be correctly styled:
<span style="background:red">backg</span><span style="background-color:red">round</span>&nbsp;<span style="color:red">foreground</span>
<small>small</small>&nbsp;<big>big</big>
<s>strikethrough</s>
<strong>bo</strong><b>ld</b>&nbsp;<em>ita</em><i>lic</i>&nbsp;<strong><em>bold&amp;</em></strong><em><strong>italic</strong></em>
<sub>sub</sub><sup>super</sup>script
<u>underline</u>
<a href="https://oxygenupdater.com/">link</a>
<script>script tag should render as plain text</script>"""

private const val TAG = "FaqScreen"

@VisibleForTesting
const val FaqScreen_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
const val FaqScreen_CategoryTextTestTag = TAG + "_CategoryText"

@VisibleForTesting
const val FaqScreen_ItemRowTestTag = TAG + "_ItemRow"

@VisibleForTesting
val PreviewFaqScreenData = listOf(
    InAppFaq(
        id = 1,
        title = PreviewTitle,
        body = null,
        type = TypeCategory,
    ),
    InAppFaq(
        id = 1,
        title = PreviewTitle,
        body = PreviewBodyHtml,
        type = TypeItem,
    ),
)

@PreviewThemes
@Composable
fun PreviewFaqScreen() = PreviewAppTheme {
    FaqScreen(
        state = RefreshAwareState(false, PreviewFaqScreenData),
        onRefresh = {},
    )
}
