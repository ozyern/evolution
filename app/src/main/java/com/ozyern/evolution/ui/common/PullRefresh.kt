package com.ozyern.evolution.ui.common

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozyern.evolution.R
import com.ozyern.evolution.ui.RefreshAwareState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PullRefresh(
    state: RefreshAwareState<T>,
    shouldShowProgressIndicator: (data: T) -> Boolean,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val refreshing = state.refreshing
    if (refreshing && shouldShowProgressIndicator(state.data)) Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifierMaxSize.testTag(PullRefresh_ProgressIndicatorTestTag)
    ) {
        ActivityIndicator(size = 28.dp)
        Text(
            text = stringResource(R.string.loading),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    } else rememberPullToRefreshState().let {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            state = it,
            indicator = {
                // `UIRefreshControl`: spokes appear one by one while pulling, then spin
                ActivityIndicator(
                    size = 28.dp,
                    revealed = { if (refreshing) 1f else it.distanceFraction },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = it.distanceFraction.coerceIn(0f, 1f)
                            translationY = (it.distanceFraction.coerceAtMost(1.5f) * 56.dp.toPx()) - 28.dp.toPx()
                        }
                        .testTag(PullRefresh_IndicatorContainerTestTag)
                )
            },
            content = content,
            modifier = Modifier.testTag(PullRefresh_ContentTestTag)
        )
    }
}

private const val TAG = "PullRefresh"

@VisibleForTesting
const val PullRefresh_ProgressIndicatorTestTag = TAG + "_ProgressIndicator"

@VisibleForTesting
const val PullRefresh_ContentTestTag = TAG + "_Content"

@VisibleForTesting
const val PullRefresh_IndicatorContainerTestTag = TAG + "_IndicatorContainer"
