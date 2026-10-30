package com.ozyern.evolution.ui.about

import com.ozyern.evolution.ComposeBaseTest
import com.ozyern.evolution.assertHasScrollAction
import com.ozyern.evolution.get
import com.ozyern.evolution.ui.main.NavType
import com.ozyern.evolution.ui.theme.PreviewWindowSize
import org.junit.Test

class AboutScreenTest : ComposeBaseTest() {

    @Test
    fun aboutScreen() {
        setContent {
            val windowWidthSize = PreviewWindowSize.widthSizeClass
            AboutScreen(
                navType = NavType.from(windowWidthSize),
                windowWidthSize = windowWidthSize,
                navigateTo = {},
                openEmail = {},
            )
        }

        rule[AboutScreen_LinksTestTag].assertExists()
        rule[AboutScreen_DescriptionTestTag].assertExists()
        rule[AboutScreen_SupportTestTag].assertExists()
        rule[AboutScreen_BackgroundStoryHeaderTestTag].assertExists()
        rule[AboutScreen_BackgroundStoryTestTag].assertExists()
        rule[AboutScreen_ThirdPartyNoticeTestTag].assertExists()

        rule[AboutScreenTestTag].run {
            assertHasScrollAction()
        }
    }
}
