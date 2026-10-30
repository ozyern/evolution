package com.ozyern.evolution.ui.common

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.ozyern.evolution.icons.Close
import com.ozyern.evolution.icons.Search
import com.ozyern.evolution.icons.Symbols

/** Blur isn't available below Android 12, so glass must be more opaque to stay legible */
private val GlassAlpha = if (SDK_INT >= VERSION_CODES.S) 0.55f else 0.92f

/** Height of [GlassSearchBar] including its outer padding, for content to leave room underneath */
val GlassSearchBarReservedHeight = 48.dp + 12.dp * 2

/**
 * iOS 26-style search field that floats at the bottom of the screen: a Liquid Glass capsule refracting
 * the content in [backdrop] (record it with [com.kyant.backdrop.backdrops.layerBackdrop]), with a
 * magnifying glass, a clear button, and a round close button that appears while editing.
 *
 * Place it at the bottom of a `Box` over the content, and apply `imePadding()` so it rides on the keyboard.
 */
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.surfaceContainerHighest.copy(alpha = GlassAlpha)
    val secondary = colorScheme.onSurfaceVariant
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val glass = Modifier.drawBackdrop(
        backdrop = backdrop,
        shape = { Capsule() },
        effects = {
            vibrancy()
            blur(8.dp.toPx())
            lens(16.dp.toPx(), 24.dp.toPx())
        },
        shadow = { Shadow(alpha = 0.5f) },
        onDrawSurface = { drawRect(containerColor) },
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .then(glass)
            .clickable(interactionSource = null, indication = null) { focusRequester.requestFocus() }
            .padding(start = 14.dp, end = 6.dp)
    ) {
        Icon(Symbols.Search, null, tint = secondary, modifier = Modifier.size(20.dp))

        BasicTextField(
            value = query,
            // Don't trim here, to allow inputting spaces
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = colorScheme.onSurface, fontSize = 17.sp),
            cursorBrush = SolidColor(colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(text = placeholder, color = secondary, fontSize = 17.sp, maxLines = 1)
                    innerTextField()
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag(GlassSearchBar_FieldTestTag)
        )

        // iOS "clear text" button: a small filled circle with an ✕
        if (query.isNotEmpty()) Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(Capsule())
                .clickable(role = Role.Button) { onQueryChange("") }
                .testTag(GlassSearchBar_ClearTestTag)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(Capsule())
                    .background(secondary.copy(alpha = 0.6f))
            ) {
                Icon(Symbols.Close, "Clear", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }

    // Round glass "close" button shown while editing: dismisses the keyboard & clears the query
    AnimatedVisibility(
        visible = focused,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .then(glass)
                .clickable(role = Role.Button) {
                    onQueryChange("")
                    focusManager.clearFocus()
                }
        ) {
            Icon(Symbols.Close, stringResource(android.R.string.cancel), tint = colorScheme.onSurface)
        }
    }
}

private const val TAG = "GlassSearchBar"

@VisibleForTesting
const val GlassSearchBar_FieldTestTag = TAG + "_Field"

@VisibleForTesting
const val GlassSearchBar_ClearTestTag = TAG + "_Clear"
