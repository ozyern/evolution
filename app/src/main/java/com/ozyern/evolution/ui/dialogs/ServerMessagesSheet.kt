package com.ozyern.evolution.ui.dialogs

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ozyern.evolution.R
import com.ozyern.evolution.icons.Error
import com.ozyern.evolution.icons.Info
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.icons.Warning
import com.ozyern.evolution.models.ServerMessage
import com.ozyern.evolution.models.ServerMessage.ServerMessagePriority
import com.ozyern.evolution.ui.common.RichText
import com.ozyern.evolution.ui.common.modifierDefaultPadding
import com.ozyern.evolution.ui.common.modifierDefaultPaddingStart
import com.ozyern.evolution.ui.theme.PreviewThemes
import com.ozyern.evolution.ui.theme.positive
import com.ozyern.evolution.ui.theme.warn

@Composable
fun ServerMessagesSheet(list: List<ServerMessage>) {
    @Suppress("NAME_SHADOWING")
    val list = if (list.isNotEmpty()) rememberSaveable(list) { list } else return

    SheetHeader(R.string.settings_push_from_server)

    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.testTag(ServerMessagesSheet_LazyColumnTestTag)
    ) {
        items(items = list, key = { it.id }, contentType = { it.priority }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifierDefaultPadding.testTag(BottomSheet_ItemRowTestTag)
            ) {
                val priority = it.priority
                val (icon, color) = remember(priority, colorScheme) {
                    when (priority) {
                        ServerMessagePriority.LOW -> Symbols.Info to colorScheme.positive
                        ServerMessagePriority.MEDIUM -> Symbols.Warning to colorScheme.warn
                        ServerMessagePriority.HIGH -> Symbols.Error to colorScheme.error
                        else -> Symbols.Info to null
                    }
                }

                Icon(icon, stringResource(R.string.icon), tint = color ?: LocalContentColor.current)
                RichText(
                    text = it.text,
                    contentColor = color ?: LocalContentColor.current,
                    modifier = modifierDefaultPaddingStart
                )
            }
        }
    }
}

private const val TAG = "ServerMessagesSheet"

@VisibleForTesting
const val ServerMessagesSheet_LazyColumnTestTag = TAG + "_LazyColumn"

@VisibleForTesting
val PreviewServerMessagesList = "An unnecessarily long server message, to get an accurate understanding of how long titles are rendered".let { message ->
    listOf(
        ServerMessage(
            1L,
            text = message,
            priority = null,
        ),
        ServerMessage(
            2L,
            text = message,
            priority = ServerMessagePriority.LOW,
        ),
        ServerMessage(
            3L,
            text = message,
            priority = ServerMessagePriority.MEDIUM,
        ),
        ServerMessage(
            4L,
            text = message,
            priority = ServerMessagePriority.HIGH,
        ),
    )
}

@PreviewThemes
@Composable
fun PreviewServerMessagesSheet() = PreviewModalBottomSheet {
    ServerMessagesSheet(PreviewServerMessagesList)
}
