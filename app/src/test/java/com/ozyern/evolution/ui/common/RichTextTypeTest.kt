package com.ozyern.evolution.ui.common

import com.ozyern.evolution.ValueClassTestHelper
import kotlin.test.Test

class RichTextTypeTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<RichTextType>()
}
