package org.example.stepik.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class StepContent(
    val text: String = "",
    val source: JsonObject? = null,
    val cost: Int = 0,
)
