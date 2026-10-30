package com.ozyern.evolution.ui.update

import com.ozyern.evolution.internal.settings.KeyUpdateMethod
import com.ozyern.evolution.ui.settings.MethodSettingsListConfig

val UpdateMethod = MethodSettingsListConfig.list[0].name!!

val GetPrefStrForUpdateMethod: (key: String, default: String) -> String = { key, default ->
    if (key == KeyUpdateMethod) UpdateMethod else default
}
