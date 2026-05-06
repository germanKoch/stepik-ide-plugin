package com.stellarflux.stepik.model

import kotlinx.serialization.Serializable

@Serializable
data class StepData(
    val id: Int,
    val position: Int,
    val type: String,
    val cost: Int = 0,
    val remote: StepContent,
    val local: StepContent? = null,
) {
    val isDirty: Boolean get() = local != null

    val effectiveContent: StepContent get() = local ?: remote
}
