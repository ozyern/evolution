package com.ozyern.evolution.ui.update

import com.ozyern.evolution.ValueClassTestHelper
import kotlin.test.Test

class DownloadFailureTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DownloadFailure>()
}
