package com.ozyern.evolution.models

/**
 * Used in [com.ozyern.evolution.ui.onboarding.OnboardingScreen]
 */
sealed interface SelectableModel {
    val id: Long
    val name: String?
    val subtitle: String?
}
