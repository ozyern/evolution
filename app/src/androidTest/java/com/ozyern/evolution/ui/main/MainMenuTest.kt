package com.ozyern.evolution.ui.main

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.unit.dp
import com.ozyern.evolution.ComposeBaseTest
import com.ozyern.evolution.assertAndPerformClick
import com.ozyern.evolution.get
import com.ozyern.evolution.models.ServerMessage
import com.ozyern.evolution.ui.dialogs.BottomSheetTestTag
import com.ozyern.evolution.ui.dialogs.PreviewServerMessagesList
import com.ozyern.evolution.ui.theme.PreviewGetPrefBool
import com.ozyern.evolution.validateColumnLayout
import org.junit.Test

class MainMenuTest : ComposeBaseTest() {

    @Test
    fun mainMenu() {
        var serverMessages by mutableStateOf<List<ServerMessage>>(listOf())
        var showMarkAllRead by mutableStateOf(false)
        setContent {
            Row {
                MainMenu(
                    serverMessages = serverMessages,
                    getPrefBool = PreviewGetPrefBool,
                    showMarkAllRead = showMarkAllRead,
                    onMarkAllReadClick = {
                        trackCallback("onMarkAllReadClick")
                    },
                    onContributorEnrollmentChange = {
                        trackCallback("onContributorEnrollmentChange: $it")
                    },
                )
            }
        }

        // First we test for the initial empty list
        rule[MainMenu_AnnouncementsButtonTestTag].assertDoesNotExist()

        // Then for non-empty and the initial value of `showMarkAllRead = false`
        serverMessages = PreviewServerMessagesList
        rule[MainMenu_DropdownMenuTestTag].assertDoesNotExist()
        rule[BottomSheetTestTag].assertDoesNotExist()
        rule[MainMenu_AnnouncementsButtonTestTag].run {
            assertWidthIsEqualTo(40.dp)
            assertAndPerformClick() // should show sheet now
        }
        rule[BottomSheetTestTag].assertExists()

        // Then for true
        showMarkAllRead = true
        rule[MainMenu_OverflowButtonTestTag].run {
            assertWidthIsEqualTo(40.dp)
            assertAndPerformClick() // should show dropdown menu now
        }
        val children = rule[MainMenu_DropdownMenuTestTag].run {
            validateColumnLayout(1)
        }

        children[0].run {
            assertAndPerformClick() // should trigger onMarkAllReadClick() and hide dropdown menu
        }
        ensureCallbackInvokedExactlyOnce("onMarkAllReadClick")
        rule[MainMenu_DropdownMenuTestTag].assertDoesNotExist()

        /**
         * TODO(test/contributor): this can be tested only on rooted devices
         *  above API 29, because checkbox & buttons are guarded behind
         *  `ContributorUtils.isAtLeastQAndPossiblyRooted`.
         *  "PossiblyRooted" is determined by Shell.isAppGrantedRoot(),
         *  which is false for emulators and managed devices.
         */
        // // Should trigger onContributorEnrollmentChange() and hide dropdown menu
        // children[1].assertAndPerformClick()
        // ensureCallbackInvokedExactlyOnce("onContributorEnrollmentChange: false")
        // composeTestRule[MainMenu_DropdownMenuTestTag].assertDoesNotExist()
        // composeTestRule[BottomSheetTestTag].assertExists()
    }
}
