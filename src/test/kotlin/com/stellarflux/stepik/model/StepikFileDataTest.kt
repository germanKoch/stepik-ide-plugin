package com.stellarflux.stepik.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StepikFileDataTest {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    private fun sampleData(): StepikFileData {
        val source = buildJsonObject {
            putJsonArray("options") {
                add(buildJsonObject {
                    put("text", "Option A")
                    put("is_correct", true)
                    put("feedback", "")
                })
            }
            put("is_multiple_choice", false)
        }

        return StepikFileData(
            courseUrl = "https://stepik.org/course/123/syllabus",
            courseId = 123,
            courseTitle = "Test Course",
            lastFetched = "2026-05-06T12:00:00Z",
            sections = listOf(
                SectionData(
                    id = 1, title = "Module 1", position = 1,
                    lessons = listOf(
                        LessonData(
                            id = 10, unitId = 100, title = "Lesson 1", position = 1,
                            steps = listOf(
                                StepData(id = 101, position = 1, type = "text", cost = 0,
                                    remote = StepContent(text = "<p>Hello</p>")),
                                StepData(id = 102, position = 2, type = "choice", cost = 5,
                                    remote = StepContent(text = "<p>Question?</p>", source = source)),
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `serialization roundtrip preserves all fields`() {
        val original = sampleData()
        val serialized = json.encodeToString(StepikFileData.serializer(), original)
        val deserialized = json.decodeFromString<StepikFileData>(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun `isDirty returns false when no local edits`() {
        assertFalse(sampleData().isDirty)
    }

    @Test
    fun `isDirty returns true when step has local content`() {
        val data = sampleData().updateStep(101) { it.copy(local = StepContent(text = "<p>Edited</p>")) }
        assertTrue(data.isDirty)
    }

    @Test
    fun `findStep returns correct step`() {
        val step = sampleData().findStep(102)
        assertEquals("choice", step?.type)
        assertEquals(5, step?.cost)
    }

    @Test
    fun `findStep returns null for unknown id`() {
        assertNull(sampleData().findStep(999))
    }

    @Test
    fun `updateStep modifies only the targeted step`() {
        val data = sampleData().updateStep(101) { it.copy(local = StepContent(text = "new")) }
        assertEquals("new", data.findStep(101)?.local?.text)
        assertNull(data.findStep(102)?.local)
    }

    @Test
    fun `dirtySteps returns only modified steps`() {
        val data = sampleData().updateStep(101) { it.copy(local = StepContent(text = "new")) }
        assertEquals(listOf(101), data.dirtySteps.map { it.id })
    }

    @Test
    fun `effectiveContent returns local when present`() {
        val step = StepData(id = 1, position = 1, type = "text", remote = StepContent(text = "remote"),
            local = StepContent(text = "local"))
        assertEquals("local", step.effectiveContent.text)
    }

    @Test
    fun `effectiveContent returns remote when local is null`() {
        val step = StepData(id = 1, position = 1, type = "text", remote = StepContent(text = "remote"))
        assertEquals("remote", step.effectiveContent.text)
    }
}
