package org.example.stepik.sync

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.stepik.api.EnvReader
import org.example.stepik.api.StepikApiClient
import org.example.stepik.api.StepikAuth
import org.example.stepik.model.StepData
import org.example.stepik.model.StepikFileData
import org.example.stepik.service.StepikProjectService

class StepikSyncService(private val project: Project) {

    fun pushAll(
        file: VirtualFile,
        fileData: StepikFileData,
        onSuccess: (StepikFileData) -> Unit,
        onConflict: (StepikFileData) -> Unit,
        onError: (String) -> Unit,
    ) {
        val dirtySteps = fileData.dirtySteps
        if (dirtySteps.isEmpty()) return

        val basePath = project.basePath ?: run { onError("Cannot determine project root."); return }
        val credentials = EnvReader.readCredentials(basePath) ?: run {
            onError("Missing STEPIK_CLIENT_ID/STEPIK_CLIENT_SECRET in .env file.")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pushing changes to Stepik...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                val auth = StepikAuth(credentials.first, credentials.second)
                val client = StepikApiClient(auth)
                val conflictDetector = ConflictDetector(client)
                val orderPreserver = OrderPreserver(client)

                indicator.text = "Checking for conflicts..."
                indicator.fraction = 0.1
                val conflicts = conflictDetector.detectConflicts(dirtySteps)
                if (conflicts.isNotEmpty()) {
                    val freshData = client.fetchFullCourse(fileData.courseId, fileData.courseUrl)
                    val service = StepikProjectService.getInstance(project)
                    service.saveFile(file, freshData)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showWarningDialog(
                            project,
                            "Conflicting steps were fetched from remote (step IDs: ${conflicts.joinToString()}).\n" +
                                "Your local copy has been updated. Please re-edit and click 'Update in Stepik' again.",
                            "Conflicts Detected",
                        )
                        onConflict(freshData)
                    }
                    return
                }

                val affectedLessons = dirtySteps
                    .mapNotNull { step -> findLessonIdForStep(fileData, step.id) }
                    .distinct()

                val originalOrders = mutableMapOf<Int, List<Pair<Int, Int>>>()
                indicator.text = "Recording step order..."
                indicator.fraction = 0.2
                for (lessonId in affectedLessons) {
                    originalOrders[lessonId] = orderPreserver.recordOrder(lessonId)
                }

                val errors = mutableListOf<String>()
                for ((i, step) in dirtySteps.withIndex()) {
                    indicator.text = "Updating step ${step.id}..."
                    indicator.fraction = 0.2 + 0.6 * (i.toDouble() / dirtySteps.size)
                    try {
                        val content = step.effectiveContent
                        val block = buildStepBlock(step.type, content.text, content.source)
                        client.updateStepSource(step.id, block)
                    } catch (e: Exception) {
                        errors.add("Step ${step.id}: ${e.message}")
                    }
                }

                indicator.text = "Restoring step order..."
                indicator.fraction = 0.85
                for (lessonId in affectedLessons) {
                    originalOrders[lessonId]?.let { orderPreserver.restoreOrder(lessonId, it) }
                }

                indicator.fraction = 1.0
                if (errors.isNotEmpty()) {
                    ApplicationManager.getApplication().invokeLater {
                        onError("Some steps failed to update:\n${errors.joinToString("\n")}")
                    }
                    return
                }

                var updatedData = fileData
                for (step in dirtySteps) {
                    updatedData = updatedData.updateStep(step.id) { s ->
                        s.copy(remote = s.effectiveContent, local = null)
                    }
                }
                val service = StepikProjectService.getInstance(project)
                service.saveFile(file, updatedData)

                ApplicationManager.getApplication().invokeLater {
                    onSuccess(updatedData)
                }
            }

            override fun onThrowable(error: Throwable) {
                ApplicationManager.getApplication().invokeLater {
                    onError("Push failed: ${error.message}")
                }
            }
        })
    }

    private fun findLessonIdForStep(data: StepikFileData, stepId: Int): Int? {
        for (section in data.sections) {
            for (lesson in section.lessons) {
                if (lesson.steps.any { it.id == stepId }) return lesson.id
            }
        }
        return null
    }

    private fun buildStepBlock(type: String, text: String, source: JsonObject?): JsonObject = buildJsonObject {
        put("name", type)
        put("text", text)
        source?.let { put("source", it) }
    }
}
