package com.ozyern.evolution.ui.main

import com.ozyern.evolution.ValueClassTestHelper
import kotlin.test.Test

class NavTypeTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<NavType>()
}
