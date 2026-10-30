package com.ozyern.evolution.ui

import com.ozyern.evolution.ValueClassTestHelper
import kotlin.test.Test

class ThemeTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<Theme>()
}
