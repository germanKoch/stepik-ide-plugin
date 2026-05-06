package org.example.stepik.editor.forms

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import org.example.stepik.model.StepContent
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class TextStepForm(project: Project) : StepForm {

    private val panel = JPanel(BorderLayout())
    private val htmlEditor: EditorTextField
    private val changeListeners = mutableListOf<() -> Unit>()

    init {
        val htmlFileType = FileTypeManager.getInstance().getFileTypeByExtension("html")
        htmlEditor = EditorTextField("", project, htmlFileType).apply {
            setOneLineMode(false)
            addSettingsProvider { editor ->
                editor.settings.apply {
                    isLineNumbersShown = true
                    isWhitespacesShown = false
                    isFoldingOutlineShown = true
                    isUseSoftWraps = true
                }
            }
        }
        htmlEditor.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                changeListeners.forEach { it() }
            }
        })
        panel.add(htmlEditor, BorderLayout.CENTER)
    }

    override fun setContent(content: StepContent) {
        htmlEditor.text = content.text
    }

    override fun getContent(): StepContent = StepContent(text = htmlEditor.text)

    override fun addChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    override val component: JComponent get() = panel
}
