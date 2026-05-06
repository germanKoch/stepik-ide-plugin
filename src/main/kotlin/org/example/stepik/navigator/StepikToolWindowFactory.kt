package org.example.stepik.navigator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.example.stepik.StepikFileType
import org.example.stepik.api.EnvReader
import org.example.stepik.api.StepikApiClient
import org.example.stepik.api.StepikAuth
import org.example.stepik.editor.StepikSplitEditor
import org.example.stepik.service.StepikProjectService
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode

class StepikToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = SimpleToolWindowPanel(true, true)
        val courseTreeModel = CourseTreeModel()
        val tree = Tree(courseTreeModel.treeModel)

        tree.addTreeSelectionListener { event: TreeSelectionEvent ->
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return@addTreeSelectionListener
            val stepNodeData = node.userObject as? CourseTreeModel.StepNodeData ?: return@addTreeSelectionListener

            val editorManager = FileEditorManager.getInstance(project)
            val currentFile = editorManager.selectedFiles.firstOrNull { it.extension == StepikFileType.EXTENSION }
                ?: return@addTreeSelectionListener

            val editor = editorManager.getEditors(currentFile)
                .filterIsInstance<StepikSplitEditor>()
                .firstOrNull() ?: return@addTreeSelectionListener

            editor.loadStep(stepNodeData.step)
        }

        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JButton("Refresh from Stepik").apply {
                addActionListener {
                    refreshFromStepik(project, courseTreeModel, tree)
                }
            })
        }

        panel.toolbar = toolbar
        panel.setContent(JBScrollPane(tree))

        val content = toolWindow.contentManager.factory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)

        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val file = event.newFile ?: return
                    if (file.extension != StepikFileType.EXTENSION) return
                    loadTree(project, file, courseTreeModel, tree)
                }
            },
        )

        val selectedFile = FileEditorManager.getInstance(project).selectedFiles
            .firstOrNull { it.extension == StepikFileType.EXTENSION }
        if (selectedFile != null) {
            loadTree(project, selectedFile, courseTreeModel, tree)
        }
    }

    private fun loadTree(project: Project, file: VirtualFile, model: CourseTreeModel, tree: Tree) {
        val service = StepikProjectService.getInstance(project)
        val data = service.loadFile(file) ?: return
        model.rebuildFromData(data)
        tree.model = model.treeModel
        expandAll(tree)
    }

    private fun refreshFromStepik(project: Project, model: CourseTreeModel, tree: Tree) {
        val service = StepikProjectService.getInstance(project)
        val editorManager = FileEditorManager.getInstance(project)
        val file = editorManager.selectedFiles.firstOrNull { it.extension == StepikFileType.EXTENSION } ?: return
        val data = service.getCached(file) ?: service.loadFile(file) ?: return
        val basePath = project.basePath ?: return
        val credentials = EnvReader.readCredentials(basePath) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Refreshing from Stepik...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val auth = StepikAuth(credentials.first, credentials.second)
                val client = StepikApiClient(auth)
                val freshData = client.fetchFullCourse(data.courseId, data.courseUrl)
                service.saveFile(file, freshData)

                ApplicationManager.getApplication().invokeLater {
                    model.rebuildFromData(freshData)
                    tree.model = model.treeModel
                    expandAll(tree)

                    editorManager.getEditors(file)
                        .filterIsInstance<StepikSplitEditor>()
                        .forEach { it.setFileData(freshData) }
                }
            }
        })
    }

    private fun expandAll(tree: Tree) {
        var i = 0
        while (i < tree.rowCount) {
            tree.expandRow(i)
            i++
        }
    }
}
