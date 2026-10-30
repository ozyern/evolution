package com.ozyern.evolution.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ozyern.evolution.R
import com.ozyern.evolution.models.ServerStatus.Status
import com.ozyern.evolution.ui.common.rememberState
import com.ozyern.evolution.ui.dialogs.AlertDialog

@Composable
fun ServerStatusDialogs(status: Status?) {
    if (status?.isNonRecoverableError != true) return

    val titleResId: Int
    val textResId: Int
    val confirmIconAndResId: Pair<ImageVector, Int>?
    val action: ((result: Boolean) -> Unit)?
    when (status) {
        Status.MAINTENANCE -> {
            titleResId = R.string.error_maintenance
            textResId = R.string.error_maintenance_message
            confirmIconAndResId = null
            action = null
        }

        // OUTDATED refers to the upstream app on Google Play, so it doesn't apply here
        else -> return
    }

    var show by rememberState(true)
    if (show) AlertDialog(
        action = {
            action?.invoke(it)
            show = false
        },
        titleResId = titleResId,
        text = stringResource(textResId),
        confirmIconAndResId = confirmIconAndResId,
    )
}
