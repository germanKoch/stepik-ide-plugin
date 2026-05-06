package com.stellarflux.stepik.service

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.stellarflux.stepik.StepikFileType
import com.stellarflux.stepik.editor.StepikSplitEditor

class StepikSaveListener(private val project: Project) : BulkFileListener {

    override fun before(events: MutableList<out VFileEvent>) {
        val editorManager = FileEditorManager.getInstance(project)
        for (file in editorManager.openFiles) {
            if (file.extension != StepikFileType.EXTENSION) continue
            editorManager.getEditors(file)
                .filterIsInstance<StepikSplitEditor>()
                .filter { it.isModified }
                .forEach { it.saveToFile() }
        }
    }
}
