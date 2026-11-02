package com.ozyern.evolution.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.RoundedRectangle
import com.ozyern.evolution.R

/** iOS-style pull-down menu: a rounded card, with separated rows. Use with [DropdownMenuItem] below. */
@Composable
fun ContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    shape = RoundedRectangle(14.dp),
    containerColor = groupedCardColor,
    tonalElevation = 0.dp,
    shadowElevation = 16.dp,
    content = content,
    modifier = modifier,
)

/**
 * A row in a [ContextMenu]: title at the leading edge, with its symbol at the trailing edge.
 *
 * @param separator whether to draw a hairline below this row; turn off for the last one
 */
@Composable
fun DropdownMenuItem(
    icon: ImageVector,
    @StringRes textResId: Int,
    separator: Boolean = true,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val hairline = colorScheme.onSurface.copy(alpha = 0.15f)
    DropdownMenuItem(
        trailingIcon = {
            Icon(icon, stringResource(R.string.icon), tint = colorScheme.onSurface)
        },
        text = {
            Text(stringResource(textResId), fontSize = 17.sp)
        },
        onClick = onClick,
        modifier = if (separator) Modifier.drawBehind {
            val y = size.height
            drawLine(hairline, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
        } else Modifier,
    )
}
