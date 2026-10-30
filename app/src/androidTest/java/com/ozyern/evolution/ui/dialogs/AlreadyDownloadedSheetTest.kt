package com.ozyern.evolution.ui.dialogs

import androidx.compose.foundation.layout.Column
import com.ozyern.evolution.R
import org.junit.Test

class AlreadyDownloadedSheetTest : ModalBottomSheetTest() {

    @Test
    fun alreadyDownloadedSheet() {
        var hidden = false
        var result: Boolean? = null
        setContent {
            Column {
                AlreadyDownloadedSheet(
                    hide = { hidden = true },
                    onClick = { result = it },
                )
            }
        }

        validateHeaderContentCaption(
            headerResIdOrString = R.string.delete_message_title,
            contentResIdOrString = R.string.delete_message_contents,
        )

        validateButtons(
            dismissResId = R.string.delete_message_delete_button,
            confirmResId = R.string.install,
            hidden = { hidden },
            result = { result },
            resetHidden = { hidden = false },
        )
    }
}
