package com.ozyern.evolution.ui.update

import androidx.compose.runtime.Immutable
import com.ozyern.evolution.ui.common.RichTextType

@Immutable
data class DownloadErrorParams(
    val text: String,
    val type: RichTextType? = null,
    val resumable: Boolean = false,
    val callback: ((resumable: Boolean) -> Unit)? = null,
)
