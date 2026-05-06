package com.stellarflux.stepik.model

import kotlinx.serialization.Serializable

@Serializable
data class StepikFileData(
    val courseUrl: String,
    val courseId: Int,
    val courseTitle: String,
    val lastFetched: String,
    val sections: List<SectionData> = emptyList(),
) {
    val isDirty: Boolean
        get() = sections.any { section ->
            section.lessons.any { lesson ->
                lesson.steps.any { it.isDirty }
            }
        }

    val allSteps: List<StepData>
        get() = sections.flatMap { it.lessons.flatMap { l -> l.steps } }

    val dirtySteps: List<StepData>
        get() = allSteps.filter { it.isDirty }

    fun findStep(stepId: Int): StepData? = allSteps.find { it.id == stepId }

    fun updateStep(stepId: Int, transform: (StepData) -> StepData): StepikFileData = copy(
        sections = sections.map { section ->
            section.copy(lessons = section.lessons.map { lesson ->
                lesson.copy(steps = lesson.steps.map { step ->
                    if (step.id == stepId) transform(step) else step
                })
            })
        }
    )
}
