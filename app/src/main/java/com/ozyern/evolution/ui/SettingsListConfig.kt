package com.ozyern.evolution.ui

import androidx.compose.runtime.Immutable
import com.ozyern.evolution.models.SelectableModel

@Immutable
data class SettingsListConfig<T : SelectableModel>(
    val list: List<T>,
    val recommendedId: Long,
    val selectedId: Long,
)
