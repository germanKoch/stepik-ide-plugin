package com.stellarflux.stepik.sync

import com.stellarflux.stepik.api.StepikApiClient
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OrderPreserver(private val client: StepikApiClient) {

    fun recordOrder(lessonId: Int): List<Pair<Int, Int>> {
        val steps = client.getSteps(lessonId)
        return steps.map { step ->
            val id = step["id"]!!.jsonPrimitive.int
            val pos = step["position"]?.jsonPrimitive?.intOrNull ?: 0
            id to pos
        }
    }

    fun restoreOrder(lessonId: Int, original: List<Pair<Int, Int>>) {
        val current = recordOrder(lessonId)
        if (current == original) return
        val currentIds = current.map { it.first }.toSet()
        for ((stepId, position) in original) {
            if (stepId in currentIds) {
                val source = client.getStepSource(stepId)
                val block = source["block"]!!.jsonObject
                client.updateStepSource(stepId, block, position)
            }
        }
    }
}
