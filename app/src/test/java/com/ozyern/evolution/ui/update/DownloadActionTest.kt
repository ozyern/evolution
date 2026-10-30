package com.ozyern.evolution.ui.update

import com.ozyern.evolution.ValueClassTestHelper
import kotlin.test.Test

class DownloadActionTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DownloadAction>()
}
