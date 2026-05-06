package org.example.stepik.sync

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.stepik.api.StepikApiClient
import org.example.stepik.model.StepData

class ConflictDetector(private val client: StepikApiClient) {

    fun detectConflicts(dirtySteps: List<StepData>): List<Int> {
        val conflicting = mutableListOf<Int>()
        for (step in dirtySteps) {
            val remoteSource = client.getStepSource(step.id)
            val remoteBlock = remoteSource["block"]?.jsonObject
            val remoteText = remoteBlock?.get("text")?.jsonPrimitive?.content ?: ""
            val remoteSourceData = remoteBlock?.get("source")?.jsonObject
            if (remoteText != step.remote.text || remoteSourceData != step.remote.source) {
                conflicting.add(step.id)
            }
        }
        return conflicting
    }
}
