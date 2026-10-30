package com.ozyern.evolution.models

import com.ozyern.evolution.ValueClassTestHelper
import kotlin.test.Test

class DeviceOsSpecTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DeviceOsSpec>()
}
