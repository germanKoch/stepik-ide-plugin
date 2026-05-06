package org.example.stepik.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.serialization.json.Json
import org.example.stepik.model.StepikFileData
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
class StepikProjectService(private val project: Project) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val cache = mutableMapOf<String, StepikFileData>()

    fun loadFile(file: VirtualFile): StepikFileData? {
        val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8).trim()
        if (content.isEmpty()) return null
        return try {
            val data = json.decodeFromString<StepikFileData>(content)
            cache[file.path] = data
            data
        } catch (e: Exception) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Stepik")
                .createNotification(
                    "Failed to parse .stepik file: ${e.message}. Reopen to re-fetch from Stepik.",
                    NotificationType.WARNING,
                )
                .notify(project)
            null
        }
    }

    fun saveFile(file: VirtualFile, data: StepikFileData) {
        val bytes = json.encodeToString(StepikFileData.serializer(), data).toByteArray(StandardCharsets.UTF_8)
        cache[file.path] = data
        val app = ApplicationManager.getApplication()
        val writeAction = Runnable {
            app.runWriteAction {
                file.setBinaryContent(bytes)
            }
        }
        if (app.isDispatchThread) {
            writeAction.run()
        } else {
            app.invokeAndWait(writeAction)
        }
    }

    fun getCached(file: VirtualFile): StepikFileData? = cache[file.path]

    fun updateCached(file: VirtualFile, data: StepikFileData) {
        cache[file.path] = data
    }

    companion object {
        fun getInstance(project: Project): StepikProjectService =
            project.getService(StepikProjectService::class.java)
    }
}
