package org.example.stepik.editor.forms

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.serialization.json.*
import org.example.stepik.model.StepContent
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.*

class StringStepForm(project: Project) : StepForm {

    private val panel = JPanel(BorderLayout())
    private val changeListeners = mutableListOf<() -> Unit>()

    private val costSpinner = JSpinner(SpinnerNumberModel(0, 0, 1000, 1))
    private val patternField = JTextField(30)
    private val useReCheckbox = JCheckBox("Regex")
    private val matchSubstringCheckbox = JCheckBox("Match substring")
    private val caseSensitiveCheckbox = JCheckBox("Case sensitive", true)

    private val htmlEditor: EditorTextField

    init {
        val settingsContent = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            val row1 = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JBLabel("Cost:"))
                add(costSpinner)
            }
            add(row1)
            val row2 = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JBLabel("Pattern:"))
                add(patternField)
                add(useReCheckbox)
                add(matchSubstringCheckbox)
                add(caseSensitiveCheckbox)
            }
            add(row2)
        }

        val collapseButton = JButton("Hide Settings").apply {
            border = JBUI.Borders.empty(2, 8)
        }
        val settingsWrapper = JPanel(BorderLayout()).apply {
            add(collapseButton, BorderLayout.NORTH)
            add(settingsContent, BorderLayout.CENTER)
        }
        collapseButton.addActionListener {
            settingsContent.isVisible = !settingsContent.isVisible
            collapseButton.text = if (settingsContent.isVisible) "Hide Settings" else "Show Settings"
            panel.revalidate()
        }

        val htmlFileType = FileTypeManager.getInstance().getFileTypeByExtension("html")
        htmlEditor = EditorTextField("", project, htmlFileType).apply {
            setOneLineMode(false)
            addSettingsProvider { editor -> editor.settings.apply { isUseSoftWraps = true; isLineNumbersShown = true } }
        }

        panel.add(settingsWrapper, BorderLayout.NORTH)
        panel.add(JBScrollPane(htmlEditor), BorderLayout.CENTER)

        costSpinner.addChangeListener { fireChange() }
        patternField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = fireChange()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = fireChange()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = fireChange()
        })
        useReCheckbox.addActionListener { fireChange() }
        matchSubstringCheckbox.addActionListener { fireChange() }
        caseSensitiveCheckbox.addActionListener { fireChange() }
        htmlEditor.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) { fireChange() }
        })
    }

    override fun setContent(content: StepContent) {
        htmlEditor.text = content.text
        val src = content.source ?: return
        patternField.text = src["pattern"]?.jsonPrimitive?.contentOrNull ?: ""
        useReCheckbox.isSelected = src["use_re"]?.jsonPrimitive?.booleanOrNull ?: false
        matchSubstringCheckbox.isSelected = src["match_substring"]?.jsonPrimitive?.booleanOrNull ?: false
        caseSensitiveCheckbox.isSelected = src["case_sensitive"]?.jsonPrimitive?.booleanOrNull ?: true
    }

    override fun getContent(): StepContent {
        val source = buildJsonObject {
            put("pattern", patternField.text)
            put("use_re", useReCheckbox.isSelected)
            put("match_substring", matchSubstringCheckbox.isSelected)
            put("case_sensitive", caseSensitiveCheckbox.isSelected)
            put("code", "")
        }
        return StepContent(text = htmlEditor.text, source = source)
    }

    override fun addChangeListener(listener: () -> Unit) { changeListeners.add(listener) }
    override val component: JComponent get() = panel

    private fun fireChange() { changeListeners.forEach { it() } }
}
