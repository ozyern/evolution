package com.ozyern.evolution.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import com.ozyern.evolution.ComposeBaseTest
import com.ozyern.evolution.R
import com.ozyern.evolution.assertAndPerformClick
import com.ozyern.evolution.get
import com.ozyern.evolution.validateRowLayout
import org.junit.Test

class CheckboxTextTest : ComposeBaseTest() {

    @Test
    fun checkboxText() {
        var checked by mutableStateOf(false)
        setContent {
            CheckboxText(
                checked = checked,
                onCheckedChange = { checked = it },
                textResId = R.string.app_name,
            )
        }

        rule[CheckboxTextTestTag].validateRowLayout(2)

        val checkboxNode = rule[CheckboxText_CheckboxTestTag].run {
            assertIsOff() // should not be checked initially
            assertAndPerformClick()
            assertIsOn() // must be checked after clicking
        }

        assert(checked) { "`checked` must be true" }

        rule[CheckboxText_TextTestTag].run {
            checkboxNode.assertIsOn() // should be checked from before
            assertAndPerformClick()
            checkboxNode.assertIsOff() // must not be checked after clicking
        }

        assert(!checked) { "`checked` must be false" }
    }
}
