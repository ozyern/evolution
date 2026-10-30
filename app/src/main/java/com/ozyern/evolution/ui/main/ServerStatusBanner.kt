package com.ozyern.evolution.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ozyern.evolution.R
import com.ozyern.evolution.icons.Error
import com.ozyern.evolution.icons.Info
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.icons.Warning
import com.ozyern.evolution.models.ServerStatus
import com.ozyern.evolution.models.ServerStatus.Status
import com.ozyern.evolution.ui.common.IconText
import com.ozyern.evolution.ui.common.modifierDefaultPadding
import com.ozyern.evolution.ui.common.modifierMaxWidth
import com.ozyern.evolution.ui.theme.PreviewAppTheme
import com.ozyern.evolution.ui.theme.PreviewThemes
import com.ozyern.evolution.ui.theme.warn

@Composable
fun ServerStatusBanner(serverStatus: ServerStatus) {
    // Note: the server's `latestAppVersion` refers to the upstream Oxygen Updater app on
    // Google Play, not this app, so we never show "new app version available" notices.
    val status = serverStatus.status
    if (status?.isUserRecoverableError != true) return

    val warn = MaterialTheme.colorScheme.warn
    val error = MaterialTheme.colorScheme.error
    val (icon, @StringRes textResId, color) = remember(status, error, warn) {
        when (status) {
            Status.WARNING -> Triple(Symbols.Warning, R.string.server_status_warning, warn)
            Status.ERROR -> Triple(Symbols.Error, R.string.server_status_error, error)
            Status.UNREACHABLE -> Triple(Symbols.Info, R.string.server_status_unreachable, error)
            else -> Triple(ImageVector.Builder("_blank_", 0.dp, 0.dp, 0f, 0f).build(), 0, null)
        }
    }

    if (color == null) return

    IconText(
        icon = icon,
        text = stringResource(textResId),
        iconTint = color,
        style = MaterialTheme.typography.bodyMedium.copy(color = color),
        modifier = modifierMaxWidth then modifierDefaultPadding
    )
    HorizontalDivider()
}

@PreviewThemes
@Composable
fun PreviewAppOutdated() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.OUTDATED, "8.8.8"))
    }
}

@PreviewThemes
@Composable
fun PreviewServerWarning() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.WARNING))
    }
}

@PreviewThemes
@Composable
fun PreviewServerError() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.ERROR))
    }
}

@PreviewThemes
@Composable
fun PreviewServerUnreachable() = PreviewAppTheme {
    Column {
        ServerStatusBanner(ServerStatus(Status.UNREACHABLE))
    }
}
