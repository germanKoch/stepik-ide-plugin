package org.example.stepik.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import org.example.stepik.api.EnvReader
import org.example.stepik.api.StepikApiClient
import org.example.stepik.api.StepikAuth
import org.example.stepik.model.StepData
import org.example.stepik.model.StepikFileData
import org.example.stepik.service.StepikProjectService
import org.example.stepik.sync.StepikSyncService
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class StepikSplitEditor(
    private val project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {

    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val service = StepikProjectService.getInstance(project)

    private var fileData: StepikFileData? = null
    private var currentStep: StepData? = null
    private var isModified = false

    private val mainPanel = JPanel(BorderLayout())
    private var splitter: JBSplitter? = null
    private var sourcePanel: StepSourcePanel? = null
    private var previewPanel: StepPreviewPanel? = null
    private var updateButton: JButton? = null

    init {
        fileData = service.loadFile(file)
        if (fileData == null) {
            showInitDialog()
        } else {
            showEditorUI()
        }
    }

    private fun showInitDialog() {
        val placeholder = JBLabel("Initializing...", SwingConstants.CENTER)
        mainPanel.add(placeholder, BorderLayout.CENTER)

        ApplicationManager.getApplication().invokeLater {
            val url = Messages.showInputDialog(
                project,
                "Enter Stepik course URL (e.g., https://stepik.org/course/123/syllabus):",
                "Initialize Stepik Course",
                null,
            )

            if (url.isNullOrBlank()) {
                mainPanel.removeAll()
                val emptyLabel = JBLabel("No course URL provided. Reopen the file to try again.", SwingConstants.CENTER)
                mainPanel.add(emptyLabel, BorderLayout.CENTER)
                mainPanel.revalidate()
                return@invokeLater
            }

            val courseId = extractCourseId(url)
            if (courseId == null) {
                mainPanel.removeAll()
                val errorLabel = JBLabel("Invalid URL. Expected format: https://stepik.org/course/{id}/...", SwingConstants.CENTER)
                mainPanel.add(errorLabel, BorderLayout.CENTER)
                mainPanel.revalidate()
                return@invokeLater
            }

            val basePath = project.basePath
            if (basePath == null) {
                Messages.showErrorDialog(project, "Cannot determine project root.", "Error")
                return@invokeLater
            }

            val credentials = EnvReader.readCredentials(basePath)
            if (credentials == null) {
                mainPanel.removeAll()
                val errorLabel = JBLabel(
                    "Missing .env file or STEPIK_CLIENT_ID/STEPIK_CLIENT_SECRET. Add them to .env in the project root.",
                    SwingConstants.CENTER,
                )
                mainPanel.add(errorLabel, BorderLayout.CENTER)
                mainPanel.revalidate()
                return@invokeLater
            }

            fetchCourse(url, courseId, credentials)
        }
    }

    private fun fetchCourse(url: String, courseId: Int, credentials: Pair<String, String>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Fetching course from Stepik...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val auth = StepikAuth(credentials.first, credentials.second)
                val client = StepikApiClient(auth)
                val data = client.fetchFullCourse(courseId, url)
                service.saveFile(file, data)
                fileData = data

                ApplicationManager.getApplication().invokeLater {
                    mainPanel.removeAll()
                    showEditorUI()
                    mainPanel.revalidate()
                    mainPanel.repaint()
                }
            }

            override fun onThrowable(error: Throwable) {
                ApplicationManager.getApplication().invokeLater {
                    mainPanel.removeAll()
                    val errorLabel = JBLabel("Failed to fetch course: ${error.message}", SwingConstants.CENTER)
                    mainPanel.add(errorLabel, BorderLayout.CENTER)
                    mainPanel.revalidate()
                }
            }
        })
    }

    private fun showEditorUI() {
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            updateButton = JButton("Update in Stepik").apply {
                isEnabled = fileData?.isDirty == true
                addActionListener { pushToStepik() }
            }
            add(updateButton)
        }
        mainPanel.add(toolbar, BorderLayout.NORTH)

        val placeholder = JBLabel("Select a step from the Stepik Navigator to begin editing.", SwingConstants.CENTER)
        mainPanel.add(placeholder, BorderLayout.CENTER)
    }

    private fun pushToStepik() {
        val data = fileData ?: return
        updateButton?.isEnabled = false
        val syncService = StepikSyncService(project)
        syncService.pushAll(
            file = file,
            fileData = data,
            onSuccess = { updatedData ->
                fileData = updatedData
                service.updateCached(file, updatedData)
                setModified(false)
                updateButton?.isEnabled = false
                Messages.showInfoMessage(project, "All changes pushed to Stepik successfully.", "Update Complete")
            },
            onConflict = { freshData ->
                fileData = freshData
                service.updateCached(file, freshData)
                setModified(false)
                updateButton?.isEnabled = false
                currentStep?.let { step ->
                    freshData.findStep(step.id)?.let { loadStep(it) }
                }
            },
            onError = { message ->
                updateButton?.isEnabled = fileData?.isDirty == true
                Messages.showErrorDialog(project, message, "Push Error")
            },
        )
    }

    fun loadStep(step: StepData) {
        currentStep = step

        val toolbar = mainPanel.getComponent(0) as? JPanel
        mainPanel.removeAll()
        if (toolbar != null) mainPanel.add(toolbar, BorderLayout.NORTH)

        sourcePanel = StepSourcePanel(project, step)
        previewPanel = StepPreviewPanel()

        val content = step.effectiveContent
        previewPanel!!.updatePreview(content.text)

        sourcePanel!!.addChangeListener {
            val updated = sourcePanel!!.getContent()
            previewPanel!!.updatePreview(updated.text)

            fileData?.let { data ->
                val newData = data.updateStep(step.id) { it.copy(local = updated) }
                fileData = newData
                service.updateCached(file, newData)
                setModified(true)
                updateButton?.isEnabled = true
            }
        }

        splitter = JBSplitter(false, 0.5f).apply {
            firstComponent = sourcePanel!!.component
            secondComponent = previewPanel!!.component
        }
        mainPanel.add(splitter!!, BorderLayout.CENTER)
        mainPanel.revalidate()
        mainPanel.repaint()
    }

    fun getFileData(): StepikFileData? = fileData

    fun setFileData(data: StepikFileData) {
        fileData = data
        service.updateCached(file, data)
        updateButton?.isEnabled = data.isDirty
    }

    fun saveToFile() {
        fileData?.let { service.saveFile(file, it) }
        setModified(false)
    }

    private fun setModified(modified: Boolean) {
        val old = isModified
        isModified = modified
        propertyChangeSupport.firePropertyChange("modified", old, modified)
    }

    override fun getComponent(): JComponent = mainPanel
    override fun getPreferredFocusedComponent(): JComponent? = sourcePanel?.component ?: mainPanel
    override fun getName(): String = "Stepik Editor"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = isModified
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }
    override fun getCurrentLocation(): FileEditorLocation? = null
    override fun dispose() {
        previewPanel?.dispose()
    }

    companion object {
        private val COURSE_URL_REGEX = Regex("""https?://stepik\.org/course/(\d+)""")

        fun extractCourseId(url: String): Int? =
            COURSE_URL_REGEX.find(url)?.groupValues?.get(1)?.toIntOrNull()
    }
}
