package com.ozyern.evolution.ui.common

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Vertically centered round checkmark (as in iOS lists) with [Text]. Tapping text toggles checked state. */
@Composable
fun CheckboxText(
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit,
    @StringRes textResId: Int,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.testTag(CheckboxTextTestTag)
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .size(44.dp)
            .testTag(CheckboxText_CheckboxTestTag)
    ) {
        RoundCheckmark(checked)
    }

    Text(
        text = stringResource(textResId),
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        modifier = textModifier
            .clickable(interactionSource, null) {
                onCheckedChange(!checked)
            }
            .testTag(CheckboxText_TextTestTag)
    )
}

@Composable
private fun RoundCheckmark(checked: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    val fill = colorScheme.primary
    val check = colorScheme.onPrimary
    val outline = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Canvas(Modifier.size(22.dp)) {
        val width = size.width
        if (!checked) {
            drawCircle(outline, style = Stroke(1.5.dp.toPx()))
            return@Canvas
        }

        drawCircle(fill)
        drawPath(
            path = Path().apply {
                moveTo(width * 0.28f, width * 0.52f)
                lineTo(width * 0.44f, width * 0.68f)
                lineTo(width * 0.73f, width * 0.35f)
            },
            color = check,
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private const val TAG = "CheckboxText"

@VisibleForTesting
const val CheckboxTextTestTag = TAG

@VisibleForTesting
const val CheckboxText_CheckboxTestTag = TAG + "_Checkbox"

@VisibleForTesting
const val CheckboxText_TextTestTag = TAG + "_Text"
