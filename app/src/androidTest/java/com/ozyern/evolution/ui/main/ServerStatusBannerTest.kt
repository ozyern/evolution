package com.ozyern.evolution.ui.main

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onChildAt
import com.ozyern.evolution.ComposeBaseTest
import com.ozyern.evolution.R
import com.ozyern.evolution.assertAndPerformClick
import com.ozyern.evolution.get
import com.ozyern.evolution.models.ServerStatus
import com.ozyern.evolution.models.ServerStatus.Status
import com.ozyern.evolution.ui.common.IconTextTestTag
import org.junit.Test

class ServerStatusBannerTest : ComposeBaseTest() {

    private val root = rule[IconTextTestTag]

    private var serverStatus by mutableStateOf(ServerStatus(Status.NORMAL))

    @Test
    fun serverStatusBanner() {
        setContent {
            ServerStatusBanner(serverStatus = serverStatus)
        }

        serverStatus = ServerStatus(Status.NORMAL)
        root.assertDoesNotExist()
        serverStatus = ServerStatus(Status.MAINTENANCE)
        root.assertDoesNotExist()

        serverStatus = ServerStatus(Status.WARNING)
        validateForRecoverableErrors(R.string.server_status_warning)

        serverStatus = ServerStatus(Status.ERROR)
        validateForRecoverableErrors(R.string.server_status_error)

        serverStatus = ServerStatus(Status.UNREACHABLE)
        validateForRecoverableErrors(R.string.server_status_unreachable)

        // Upstream app version notices never apply to this app
        serverStatus = ServerStatus(Status.OUTDATED, latestAppVersion = "88.88.88")
        root.assertDoesNotExist()
        serverStatus = ServerStatus(latestAppVersion = "88.88.88")
        root.assertDoesNotExist()
    }

    private fun validateForRecoverableErrors(@StringRes resId: Int) {
        root.onChildAt(1).assertHasTextExactly(resId)
        ensureNoCallbacksWereInvoked()
    }
}
