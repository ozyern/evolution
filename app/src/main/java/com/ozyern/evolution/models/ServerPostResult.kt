package com.ozyern.evolution.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Immutable
data class ServerPostResult(
    val success: Boolean = false,

    /** Only used for [com.ozyern.evolution.apis.ServerApi.getFreshUpdateDataDownloadUrl] */
    val result: String? = null,

    @JsonNames("error_message")
    val errorMessage: String? = null,
)
