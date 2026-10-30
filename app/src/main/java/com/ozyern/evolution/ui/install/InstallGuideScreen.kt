package com.ozyern.evolution.ui.install

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ozyern.evolution.R
import com.ozyern.evolution.models.InstallGuide
import com.ozyern.evolution.ui.RefreshAwareState
import com.ozyern.evolution.ui.common.Chevron
import com.ozyern.evolution.ui.common.ExpandCollapse
import com.ozyern.evolution.ui.common.ListItemTextIndent
import com.ozyern.evolution.ui.common.PullRefresh
import com.ozyern.evolution.ui.common.RichText
import com.ozyern.evolution.ui.common.animatedClickable
import com.ozyern.evolution.ui.common.groupedBackground
import com.ozyern.evolution.ui.common.groupedCell
import com.ozyern.evolution.ui.common.modifierMaxWidth
import com.ozyern.evolution.ui.common.withPlaceholder
import com.ozyern.evolution.ui.theme.PreviewAppTheme
import com.ozyern.evolution.ui.theme.PreviewThemes

@Composable
fun InstallGuideScreen(
    viewModel: InstallGuideViewModel,
    downloaded: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InstallGuideScreen(
        state = state,
        onRefresh = viewModel::refresh,
        downloaded = downloaded,
    )
}

@VisibleForTesting
@Composable
fun InstallGuideScreen(
    state: RefreshAwareState<List<InstallGuide>>,
    onRefresh: () -> Unit,
    downloaded: Boolean,
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

        LazyColumn(
            Modifier
                .fillMaxSize()
                .testTag(InstallGuideScreen_LazyColumnTestTag)
        ) {
            // Show download instructions only if user hasn't downloaded yet
            if (!downloaded) item {
                val bodyMedium = MaterialTheme.typography.bodyMedium
                Text(
                    text = AnnotatedString(
                        stringResource(R.string.install_guide_download_instructions),
                        bodyMedium.toSpanStyle(),
                        bodyMedium.toParagraphStyle().copy(textIndent = ListItemTextIndent)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = bodyMedium,
                    modifier = Modifier
                        .padding(start = 32.dp, end = 32.dp, top = 16.dp)
                        .testTag(InstallGuideScreen_InstructionsTestTag)
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            itemsIndexed(items = list, key = { _, it -> it.id }) { index, it ->
                InstallGuideItem(
                    refreshing = refreshing,
                    item = it,
                    number = index + 1,
                    first = index == 0,
                    last = index == lastIndex,
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

/** A numbered, expandable step in one grouped card, with an iOS disclosure chevron that rotates when open */
@Composable
private fun InstallGuideItem(
    refreshing: Boolean,
    item: InstallGuide,
    number: Int,
    first: Boolean,
    last: Boolean,
) = Column(Modifier.groupedCell(first = first, last = last, separatorStart = 58.dp)) {
    var expanded by remember { item.expanded }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "GuideChevronRotation")
    val colorScheme = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifierMaxWidth
            .animatedClickable { expanded = !expanded }
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp) // must be after `clickable`
            .testTag(InstallGuideScreen_ItemRowTestTag)
    ) {
        // Step number in a filled circle, like iOS setup checklists
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colorScheme.primary)
        ) {
            Text(number.toString(), color = colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 17.sp,
                modifier = Modifier
                    .withPlaceholder(refreshing)
                    .testTag(InstallGuideScreen_ItemTitleTestTag)
            )

            Text(
                text = item.subtitle,
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier
                    .withPlaceholder(refreshing)
                    .testTag(InstallGuideScreen_ItemSubtitleTestTag)
            )
        }

        Box(Modifier.rotate(rotation)) { Chevron() }
    }

    ExpandCollapse(expanded) {
        RichText(
            text = item.body,
            textIndent = ListItemTextIndent,
            contentColor = colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 58.dp, end = 16.dp, bottom = 14.dp)
                .withPlaceholder(refreshing)
        )
    }
}

private const val PreviewTitle = "An unnecessarily long guide entry, to get an accurate understanding of how long text is rendered"
private const val PreviewBodyPrefix = "More information about this guide entry"
private const val PreviewBodyHtml = """HTML markup, should be correctly styled:
<span style="background:red">backg</span><span style="background-color:red">round</span>&nbsp;<span style="color:red">foreground</span>
<small>small</small>&nbsp;<big>big</big>
<s>strikethrough</s>
<strong>bo</strong><b>ld</b>&nbsp;<em>ita</em><i>lic</i>&nbsp;<strong><em>bold&amp;</em></strong><em><strong>italic</strong></em>
<sub>sub</sub><sup>super</sup>script
<u>underline</u>
<a href="https://oxygenupdater.com/">link</a>
<script>script tag should render as plain text</script>"""

private const val TAG = "InstallGuideScreen"

@VisibleForTesting
const val InstallGuideScreen_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
const val InstallGuideScreen_InstructionsTestTag = TAG + "_Instructions"

@VisibleForTesting
const val InstallGuideScreen_ItemRowTestTag = TAG + "_ItemRow"

@VisibleForTesting
const val InstallGuideScreen_ItemTitleTestTag = TAG + "_ItemTitle"

@VisibleForTesting
const val InstallGuideScreen_ItemSubtitleTestTag = TAG + "_ItemSubtitle"

@VisibleForTesting
val PreviewInstallGuideScreenData = listOf(
    InstallGuide(
        id = 1,
        title = PreviewTitle,
        subtitle = PreviewBodyPrefix,
        body = PreviewBodyHtml,
    ),
    InstallGuide(
        id = 2,
        title = PreviewTitle,
        subtitle = PreviewBodyPrefix,
        body = PreviewBodyHtml,
    ),
)

@PreviewThemes
@Composable
fun PreviewInstallGuideScreen() = PreviewAppTheme {
    InstallGuideScreen(
        state = RefreshAwareState(false, PreviewInstallGuideScreenData),
        onRefresh = {},
        downloaded = false,
    )
}
