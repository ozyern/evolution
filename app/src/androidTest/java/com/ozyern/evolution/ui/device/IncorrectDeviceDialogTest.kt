package com.ozyern.evolution.ui.device

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onParent
import com.ozyern.evolution.ComposeBaseTest
import com.ozyern.evolution.R
import com.ozyern.evolution.get
import com.ozyern.evolution.ui.dialogs.AlertDialogTestTag
import com.ozyern.evolution.ui.dialogs.AlertDialog_TextTestTag
import org.junit.Test

class IncorrectDeviceDialogTest : ComposeBaseTest() {

    @Test
    fun incorrectDeviceDialog() {
        var mismatchStatus by mutableStateOf(Triple(false, "correct", "correct"))
        setContent {
            IncorrectDeviceDialog(
                hide = {},
                mismatchStatus = mismatchStatus,
            )
        }

        // First we test for the initial value of false
        rule[AlertDialogTestTag].assertDoesNotExist()

        // Then for true
        mismatchStatus = Triple(true, "incorrect", "correct")
        rule[AlertDialogTestTag].run {
            assertExists()
            onParent().assert(isDialog())
        }
        rule[AlertDialog_TextTestTag].assertHasTextExactly(
            activity.getString(R.string.incorrect_device_warning_message, mismatchStatus.second, mismatchStatus.third),
        )
    }
}
