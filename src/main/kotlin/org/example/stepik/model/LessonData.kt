package org.example.stepik.model

import kotlinx.serialization.Serializable

@Serializable
data class LessonData(
    val id: Int,
    val unitId: Int,
    val title: String,
    val position: Int,
    val steps: List<StepData> = emptyList(),
)
