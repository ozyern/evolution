package com.ozyern.evolution.ui.dialogs

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ozyern.evolution.R
import com.ozyern.evolution.extensions.showToast
import com.ozyern.evolution.icons.CheckCircle
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.internal.settings.KeyContribute
import com.ozyern.evolution.ui.common.CheckboxText
import com.ozyern.evolution.ui.common.rememberState
import com.ozyern.evolution.ui.theme.PreviewGetPrefBool
import com.ozyern.evolution.ui.theme.PreviewThemes
import com.ozyern.evolution.utils.ContributorUtils
import com.ozyern.evolution.utils.hasRootAccess

@Composable
fun ColumnScope.ContributorSheet(
    hide: () -> Unit,
    getPrefBool: ((key: String, default: Boolean) -> Boolean)? = null,
    confirm: ((Boolean) -> Unit)? = null,
) {
    SheetHeader(R.string.contribute_title)

    Text(
        text = stringResource(R.string.contribute_explanation),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
            .testTag(BottomSheet_ContentTestTag)
    )

    /**
     * Show enrollment UI only if [confirm] was passed in, and if
     * device is rooted with the OS being >= Android 10/Q.
     */
    if (confirm != null && ContributorUtils.isAtLeastQAndPossiblyRooted) {
        ContributorSheetEnroll(hide = hide, getPrefBool = getPrefBool, confirm = confirm)
    }
}

@Composable
private fun ContributorSheetEnroll(
    hide: () -> Unit,
    getPrefBool: ((key: String, default: Boolean) -> Boolean)? = null,
    confirm: (Boolean) -> Unit,
) {
    var contribute by rememberState(getPrefBool?.invoke(KeyContribute, false) != false)
    CheckboxText(
        checked = contribute,
        onCheckedChange = { contribute = it },
        textResId = R.string.contribute_agree,
        modifier = Modifier.padding(end = 16.dp)
    )

    val context = LocalContext.current
    SheetButtons(
        dismissResId = android.R.string.cancel,
        onDismiss = hide,
        confirmIcon = Symbols.CheckCircle,
        confirmResId = R.string.contribute_save,
        onConfirm = {
            if (contribute) hasRootAccess {
                if (it) {
                    confirm(true); hide()
                } else context.showToast(R.string.contribute_allow_storage)
            } else {
                confirm(false); hide()
            }
        },
    )
}

@PreviewThemes
@Composable
fun PreviewContributorSheet() = PreviewModalBottomSheet {
    ContributorSheet(hide = {}, getPrefBool = PreviewGetPrefBool, confirm = {})
}
