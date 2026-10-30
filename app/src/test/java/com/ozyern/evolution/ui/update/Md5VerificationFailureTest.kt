package com.ozyern.evolution.ui.update

import com.ozyern.evolution.ValueClassTestHelper
import kotlin.test.Test

class Md5VerificationFailureTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<Md5VerificationFailure>()
}
