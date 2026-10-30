package com.ozyern.evolution.ui.dialogs

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ozyern.evolution.ui.common.GroupedFooter
import com.ozyern.evolution.ui.common.IconTextButton
import com.ozyern.evolution.ui.common.groupedBackground
import com.ozyern.evolution.ui.common.modifierMaxWidth
import com.ozyern.evolution.ui.theme.PreviewAppTheme

/**
 * [hide] is automatically called when user presses back button.
 * See [androidx.compose.material3.ModalBottomSheetWindow.dispatchKeyEvent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NonRestartableComposable
fun ModalBottomSheet(
    hide: () -> Unit,
    content: @Composable ColumnScope.(() -> Unit) -> Unit,
) = ModalBottomSheet(
    onDismissRequest = hide,
    sheetState = rememberModalBottomSheetState(true),
    containerColor = groupedBackground,
    modifier = Modifier.testTag(BottomSheetTestTag)
) { content(hide) }

/** Centred, semibold title, like an iOS sheet's navigation bar */
@Composable
@NonRestartableComposable
fun SheetHeader(@StringRes titleResId: Int) = Text(
    text = stringResource(titleResId),
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold,
    textAlign = TextAlign.Center,
    maxLines = 1,
    modifier = modifierMaxWidth
        .basicMarquee()
        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        .testTag(BottomSheet_HeaderTestTag)
)

@Composable
fun SheetButtons(
    @StringRes dismissResId: Int,
    onDismiss: () -> Unit,
    confirmIcon: ImageVector,
    @StringRes confirmResId: Int,
    onConfirm: (() -> Unit)?,
    modifier: Modifier = Modifier,
) = Row(
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
    modifier = (modifier then modifierMaxWidth).padding(
        start = 16.dp,
        top = 12.dp,
        end = 16.dp,
        bottom = 16.dp,
    )
) {
    TextButton(
        onClick = onDismiss,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = (if (onConfirm != null) Modifier.padding(end = 16.dp) else Modifier)
            .testTag(BottomSheet_DismissButtonTestTag),
    ) {
        Text(stringResource(dismissResId))
    }

    if (onConfirm != null) IconTextButton(
        onClick = onConfirm,
        icon = confirmIcon,
        textResId = confirmResId,
    )
}

@Composable
@NonRestartableComposable
fun SheetCaption(@StringRes captionResId: Int) = GroupedFooter(
    text = stringResource(captionResId),
    modifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(bottom = 16.dp)
        .testTag(BottomSheet_CaptionTestTag)
)

private const val TAG = "ModalBottomSheet"

@VisibleForTesting
const val BottomSheetTestTag = TAG

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_HeaderTestTag = TAG + "_Header"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_ContentTestTag = TAG + "_Content"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_CaptionTestTag = TAG + "_Caption"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_DismissButtonTestTag = TAG + "_DismissButton"

@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val BottomSheet_ItemRowTestTag = TAG + "_ItemRow"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewModalBottomSheet(content: @Composable ColumnScope.() -> Unit) = PreviewAppTheme {
    val density = LocalDensity.current
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = SheetState(
            skipPartiallyExpanded = true,
            /** [androidx.compose.material3.BottomSheetDefaults.PositionalThreshold] */
            positionalThreshold = { with(density) { 56.dp.toPx() } },
            /** [androidx.compose.material3.BottomSheetDefaults.VelocityThreshold] */
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            initialValue = SheetValue.Expanded,
        ),
        content = content
    )
}
