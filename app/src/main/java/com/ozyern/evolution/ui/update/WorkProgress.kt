package com.ozyern.evolution.ui.update

import androidx.compose.runtime.Immutable

@Immutable
data class WorkProgress(
    val bytesDone: Long,
    val totalBytes: Long,
    val currentProgress: Int,
    val downloadEta: String?,
)
