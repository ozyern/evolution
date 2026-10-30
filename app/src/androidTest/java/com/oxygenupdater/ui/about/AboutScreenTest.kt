package com.oxygenupdater.ui.about

import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.assertHasScrollAction
import com.oxygenupdater.get
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.theme.PreviewWindowSize
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
