package com.stellarflux.stepik.api

import com.intellij.util.io.HttpRequests
import com.stellarflux.stepik.model.LessonData
import com.stellarflux.stepik.model.SectionData
import com.stellarflux.stepik.model.StepContent
import com.stellarflux.stepik.model.StepData
import com.stellarflux.stepik.model.StepikFileData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.time.Instant

class StepikApiClient(private val auth: com.stellarflux.stepik.api.StepikAuth) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun get(path: String, params: String = ""): JsonObject {
        val url = if (params.isEmpty()) "$BASE_URL/$path" else "$BASE_URL/$path?$params"
        return executeWithRetry { token ->
            HttpRequests.request(url)
                .accept("application/json")
                .tuner { it.setRequestProperty("Authorization", "Bearer $token") }
                .readString()
        }
    }

    private fun put(path: String, body: JsonObject): JsonObject {
        val url = "$BASE_URL/$path"
        return executeWithRetry { token ->
            HttpRequests.put(url, "application/json")
                .tuner { it.setRequestProperty("Authorization", "Bearer $token") }
                .connect { request ->
                    request.write(body.toString())
                    request.readString()
                }
        }
    }

    private fun executeWithRetry(request: (token: String) -> String): JsonObject {
        try {
            val response = request(auth.getToken())
            return json.parseToJsonElement(response).jsonObject
        } catch (e: com.intellij.util.io.HttpRequests.HttpStatusException) {
            if (e.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                auth.invalidateToken()
                val response = request(auth.getToken())
                return json.parseToJsonElement(response).jsonObject
            }
            throw e
        }
    }

    fun getCourse(courseId: Int): JsonObject {
        val result = get("courses/$courseId")
        return result["courses"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw RuntimeException("Course $courseId not found")
    }

    fun getSections(sectionIds: List<Int>): List<JsonObject> {
        if (sectionIds.isEmpty()) return emptyList()
        val params = sectionIds.joinToString("&") { "ids[]=$it" }
        val result = get("sections", params)
        return result["sections"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    }

    fun getUnits(unitIds: List<Int>): List<JsonObject> {
        if (unitIds.isEmpty()) return emptyList()
        val params = unitIds.joinToString("&") { "ids[]=$it" }
        val result = get("units", params)
        return result["units"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    }

    fun getLesson(lessonId: Int): JsonObject {
        val result = get("lessons/$lessonId")
        return result["lessons"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw RuntimeException("Lesson $lessonId not found")
    }

    fun getSteps(lessonId: Int): List<JsonObject> {
        val result = get("steps", "lesson=$lessonId")
        return result["steps"]?.jsonArray?.map { it.jsonObject }
            ?.sortedBy { it["position"]?.jsonPrimitive?.intOrNull ?: 0 }
            ?: emptyList()
    }

    fun getStepSource(stepId: Int): JsonObject {
        val result = get("step-sources/$stepId")
        return result["step-sources"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw RuntimeException("Step-source $stepId not found")
    }

    fun updateStepSource(stepId: Int, block: JsonObject, position: Int? = null, cost: Int? = null): JsonObject {
        val fields = buildJsonObject {
            put("block", block)
            position?.let { put("position", JsonPrimitive(it)) }
            cost?.let { put("cost", JsonPrimitive(it)) }
        }
        val body = buildJsonObject { put("step-source", fields) }
        val result = put("step-sources/$stepId", body)
        return result["step-sources"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw RuntimeException("Failed to update step-source $stepId")
    }

    fun fetchFullCourse(courseId: Int, courseUrl: String): com.stellarflux.stepik.model.StepikFileData {
        val course = getCourse(courseId)
        val courseTitle = course["title"]?.jsonPrimitive?.content ?: "Untitled"
        val sectionIds = course["sections"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()

        val sections = getSections(sectionIds)
            .sortedBy { it["position"]?.jsonPrimitive?.intOrNull ?: 0 }
            .map { section ->
                val sectionId = section["id"]!!.jsonPrimitive.int
                val unitIds = section["units"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList()
                val units = getUnits(unitIds).sortedBy { it["position"]?.jsonPrimitive?.intOrNull ?: 0 }

                val lessons = units.map { unit ->
                    val lessonId = unit["lesson"]!!.jsonPrimitive.int
                    val unitId = unit["id"]!!.jsonPrimitive.int
                    val unitPosition = unit["position"]?.jsonPrimitive?.intOrNull ?: 0
                    val lesson = getLesson(lessonId)
                    val lessonTitle = lesson["title"]?.jsonPrimitive?.content ?: "Untitled"

                    val steps = getSteps(lessonId).map { step ->
                        val stepId = step["id"]!!.jsonPrimitive.int
                        val stepPosition = step["position"]?.jsonPrimitive?.intOrNull ?: 0
                        val block = step["block"]?.jsonObject
                        val stepType = block?.get("name")?.jsonPrimitive?.content ?: "text"
                        val stepText = block?.get("text")?.jsonPrimitive?.content ?: ""

                        val source = getStepSource(stepId)
                        val sourceBlock = source["block"]?.jsonObject
                        val sourceData = sourceBlock?.get("source")?.jsonObject
                        val stepCost = source["cost"]?.jsonPrimitive?.intOrNull ?: 0
                        val fullText = sourceBlock?.get("text")?.jsonPrimitive?.content ?: stepText

                        StepData(
                            id = stepId,
                            position = stepPosition,
                            type = stepType,
                            cost = stepCost,
                            remote = StepContent(text = fullText, source = sourceData, cost = stepCost),
                        )
                    }

                    LessonData(
                        id = lessonId,
                        unitId = unitId,
                        title = lessonTitle,
                        position = unitPosition,
                        steps = steps,
                    )
                }

                SectionData(
                    id = sectionId,
                    title = section["title"]?.jsonPrimitive?.content ?: "Untitled",
                    position = section["position"]?.jsonPrimitive?.intOrNull ?: 0,
                    lessons = lessons,
                )
            }

        return StepikFileData(
            courseUrl = courseUrl,
            courseId = courseId,
            courseTitle = courseTitle,
            lastFetched = Instant.now().toString(),
            sections = sections,
        )
    }

    companion object {
        private const val BASE_URL = "https://stepik.org/api"
    }
}
