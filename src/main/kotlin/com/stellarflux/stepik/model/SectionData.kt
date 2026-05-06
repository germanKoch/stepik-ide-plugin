package com.stellarflux.stepik.model

import kotlinx.serialization.Serializable

@Serializable
data class SectionData(
    val id: Int,
    val title: String,
    val position: Int,
    val lessons: List<LessonData> = emptyList(),
)
