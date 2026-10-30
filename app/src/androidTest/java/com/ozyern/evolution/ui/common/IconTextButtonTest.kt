package com.ozyern.evolution.ui.common

import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import com.ozyern.evolution.ComposeBaseTest
import com.ozyern.evolution.R
import com.ozyern.evolution.assertAndPerformClick
import com.ozyern.evolution.assertSizeIsEqualTo
import com.ozyern.evolution.get
import com.ozyern.evolution.icons.Android
import com.ozyern.evolution.icons.Symbols
import com.ozyern.evolution.validateRowLayout
import org.junit.Test

class IconTextButtonTest : ComposeBaseTest() {

    @Test
    fun iconTextButton() {
        setContent {
            IconTextButton(
                onClick = { trackCallback("onClick") },
                icon = Symbols.Android,
                textResId = R.string.app_name,
            )
        }

        rule[IconTextButtonTestTag].run {
            assertAndPerformClick()
            ensureCallbackInvokedExactlyOnce("onClick")
        }

        // Icon
        rule[IconTextButton_IconTestTag, true].run {
            assertSizeIsEqualTo(18.dp)
            onParent().validateRowLayout(2)
        }

        // Text
        rule[IconTextButton_TextTestTag, true].run {
            assertHasTextExactly(R.string.app_name)
            onParent().validateRowLayout(2)
        }
    }
}
