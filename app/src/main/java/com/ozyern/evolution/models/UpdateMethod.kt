package com.ozyern.evolution.models

import androidx.compose.runtime.Immutable
import com.ozyern.evolution.internal.ForceBoolean
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Immutable
data class UpdateMethod(
    override val id: Long = 0,
    override val name: String? = null,

    @JsonNames("recommended_for_rooted_device")
    val recommendedForRootedDevice: ForceBoolean = false,

    @JsonNames("recommended_for_non_rooted_device")
    val recommendedForNonRootedDevice: ForceBoolean = false,

    @JsonNames("supports_rooted_device")
    val supportsRootedDevice: ForceBoolean = false,
) : SelectableModel {
    override val subtitle = null
}
