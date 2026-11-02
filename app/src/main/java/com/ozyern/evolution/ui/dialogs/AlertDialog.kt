package com.ozyern.evolution.ui.dialogs

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kyant.shapes.RoundedRectangle
import com.ozyern.evolution.R
import com.ozyern.evolution.ui.common.groupedCardColor

/**
 * iOS-style alert (`UIAlertController`): a narrow card with a centred title & message, and text-only
 * buttons in a row below a hairline.
 *
 * @param confirmIconAndResId the confirm button's label. iOS alert buttons are text-only, so the icon isn't drawn.
 */
@Composable
fun AlertDialog(
    action: (result: Boolean) -> Unit,
    @StringRes titleResId: Int,
    text: String,
    confirmIconAndResId: Pair<ImageVector, Int>? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) = Dialog(
    onDismissRequest = { action(false) },
    properties = NonCancellableDialog,
) {
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    Column(
        Modifier
            .width(AlertWidth)
            .clip(RoundedRectangle(14.dp))
            .background(groupedCardColor)
            .testTag(AlertDialogTestTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(titleResId),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(AlertDialog_TitleTestTag)
            )

            val message = @Composable {
                Text(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag(AlertDialog_TextTestTag)
                )
            }
            if (content != null) Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.testTag(AlertDialog_ContentTestTag)
            ) {
                message()
                content()
            } else message()
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(hairline)
        )

        Row(Modifier.height(IntrinsicSize.Min)) {
            val confirm = confirmIconAndResId?.second
            AlertButton(
                text = stringResource(R.string.download_error_close),
                // The only button in an alert is its default action, which iOS emphasises
                emphasised = confirm == null,
                onClick = { action(false) },
                modifier = Modifier.testTag(AlertDialog_DismissButtonTestTag)
            )

            if (confirm != null) {
                Box(
                    Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(hairline)
                )
                AlertButton(
                    text = stringResource(confirm),
                    emphasised = true,
                    onClick = { action(true) },
                    modifier = Modifier.testTag(AlertDialog_ConfirmButtonTestTag)
                )
            }
        }
    }
}

@Composable
private fun RowScope.AlertButton(
    text: String,
    emphasised: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
        .weight(1f)
        .heightIn(min = 44.dp)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 10.dp)
) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 17.sp,
        fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

/** Same as iOS alerts */
private val AlertWidth = 270.dp

private const val TAG = "AlertDialog"

@VisibleForTesting
const val AlertDialogTestTag = TAG

@VisibleForTesting
const val AlertDialog_DismissButtonTestTag = TAG + "_DismissButton"

@VisibleForTesting
const val AlertDialog_ConfirmButtonTestTag = TAG + "_ConfirmButton"

@VisibleForTesting
const val AlertDialog_TitleTestTag = TAG + "_Title"

@VisibleForTesting
const val AlertDialog_TextTestTag = TAG + "_Text"

@VisibleForTesting
const val AlertDialog_ContentTestTag = TAG + "_Content"
