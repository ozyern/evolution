package com.ozyern.evolution.ui.common

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import com.ozyern.evolution.R
import com.ozyern.evolution.icons.Android
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.ui.theme.PreviewAppTheme
import com.ozyern.evolution.ui.theme.PreviewThemes

/**
 * iOS-style capsule button with a leading [icon] followed by [Text].
 *
 * @param filled if true (default), a prominent button (primary bg); otherwise, a tinted one.
 */
@Composable
fun IconTextButton(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes textResId: Int,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .heightIn(min = 44.dp)
        .clip(Capsule())
        .background(MaterialTheme.colorScheme.primary.let { if (filled) it else it.copy(alpha = 0.12f) })
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 10.dp)
        .testTag(IconTextButtonTestTag)
) {
    val contentColor = MaterialTheme.colorScheme.run { if (filled) onPrimary else primary }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier
            .requiredSize(18.dp)
            .testTag(IconTextButton_IconTestTag)
    )
    Text(
        text = stringResource(textResId),
        color = contentColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.testTag(IconTextButton_TextTestTag)
    )
}

private const val TAG = "IconTextButton"

@VisibleForTesting
const val IconTextButtonTestTag = TAG

@VisibleForTesting
const val IconTextButton_IconTestTag = TAG + "_Icon"

@VisibleForTesting
const val IconTextButton_TextTestTag = TAG + "_Text"

@PreviewThemes
@Composable
fun PreviewIconTextButton() = PreviewAppTheme {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(true, false).forEach {
            IconTextButton(
                onClick = {},
                icon = Symbols.Android,
                textResId = R.string.app_name,
                filled = it,
            )
        }
    }
}
