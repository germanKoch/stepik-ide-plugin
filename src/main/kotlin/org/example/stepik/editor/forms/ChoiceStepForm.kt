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
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ChoiceStepForm(project: Project) : StepForm {

    private val panel = JPanel(BorderLayout())
    private val changeListeners = mutableListOf<() -> Unit>()

    private val costSpinner = JSpinner(SpinnerNumberModel(0, 0, 1000, 1))
    private val multipleChoiceCheckbox = JCheckBox("Multiple choice")
    private val preserveOrderCheckbox = JCheckBox("Preserve order")
    private val feedbackCorrectField = JTextField(30)
    private val feedbackWrongField = JTextField(30)

    private val tableModel = object : DefaultTableModel(arrayOf("Text", "Correct", "Feedback"), 0) {
        override fun getColumnClass(column: Int): Class<*> =
            if (column == 1) java.lang.Boolean::class.java else String::class.java
        override fun isCellEditable(row: Int, column: Int): Boolean = true
    }
    private val optionsTable = JTable(tableModel)

    private val htmlEditor: EditorTextField

    init {
        val metaPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            val row1 = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JBLabel("Cost:"))
                add(costSpinner)
                add(multipleChoiceCheckbox)
                add(preserveOrderCheckbox)
            }
            add(row1)

            val row2 = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JBLabel("Correct feedback:"))
                add(feedbackCorrectField)
            }
            add(row2)

            val row3 = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JBLabel("Wrong feedback:"))
                add(feedbackWrongField)
            }
            add(row3)
        }

        val tablePanel = JPanel(BorderLayout()).apply {
            add(JBLabel("Options:"), BorderLayout.NORTH)
            add(JBScrollPane(optionsTable).apply { preferredSize = Dimension(0, 150) }, BorderLayout.CENTER)
            val buttons = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                val addBtn = JButton("Add").apply {
                    addActionListener {
                        tableModel.addRow(arrayOf<Any>("", false, ""))
                        fireChange()
                    }
                }
                val removeBtn = JButton("Remove").apply {
                    addActionListener {
                        val row = optionsTable.selectedRow
                        if (row >= 0) { tableModel.removeRow(row); fireChange() }
                    }
                }
                add(addBtn)
                add(removeBtn)
            }
            add(buttons, BorderLayout.SOUTH)
        }

        val settingsContent = JPanel(BorderLayout()).apply {
            add(metaPanel, BorderLayout.NORTH)
            add(tablePanel, BorderLayout.CENTER)
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
            addSettingsProvider { editor ->
                editor.settings.apply { isUseSoftWraps = true; isLineNumbersShown = true }
            }
        }

        panel.add(settingsWrapper, BorderLayout.NORTH)
        panel.add(JBScrollPane(htmlEditor), BorderLayout.CENTER)

        costSpinner.addChangeListener { fireChange() }
        multipleChoiceCheckbox.addActionListener { fireChange() }
        preserveOrderCheckbox.addActionListener { fireChange() }
        feedbackCorrectField.document.addDocumentListener(SimpleDocListener { fireChange() })
        feedbackWrongField.document.addDocumentListener(SimpleDocListener { fireChange() })
        tableModel.addTableModelListener { fireChange() }
        htmlEditor.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) { fireChange() }
        })
    }

    override fun setContent(content: StepContent) {
        htmlEditor.text = content.text
        val src = content.source ?: return
        costSpinner.value = 0
        multipleChoiceCheckbox.isSelected = src["is_multiple_choice"]?.jsonPrimitive?.booleanOrNull ?: false
        preserveOrderCheckbox.isSelected = src["preserve_order"]?.jsonPrimitive?.booleanOrNull ?: false
        feedbackCorrectField.text = src["feedback_correct"]?.jsonPrimitive?.contentOrNull ?: ""
        feedbackWrongField.text = src["feedback_wrong"]?.jsonPrimitive?.contentOrNull ?: ""

        tableModel.rowCount = 0
        src["options"]?.jsonArray?.forEach { opt ->
            val o = opt.jsonObject
            tableModel.addRow(arrayOf<Any>(
                o["text"]?.jsonPrimitive?.contentOrNull ?: "",
                o["is_correct"]?.jsonPrimitive?.booleanOrNull ?: false,
                o["feedback"]?.jsonPrimitive?.contentOrNull ?: "",
            ))
        }
    }

    override fun getContent(): StepContent {
        val options = buildJsonArray {
            for (i in 0 until tableModel.rowCount) {
                add(buildJsonObject {
                    put("text", tableModel.getValueAt(i, 0)?.toString() ?: "")
                    put("is_correct", tableModel.getValueAt(i, 1) as? Boolean ?: false)
                    put("feedback", tableModel.getValueAt(i, 2)?.toString() ?: "")
                })
            }
        }
        val source = buildJsonObject {
            put("options", options)
            put("is_multiple_choice", multipleChoiceCheckbox.isSelected)
            put("preserve_order", preserveOrderCheckbox.isSelected)
            put("sample_size", tableModel.rowCount)
            put("is_options_feedback", options.any { it.jsonObject["feedback"]?.jsonPrimitive?.content?.isNotEmpty() == true })
            put("feedback_correct", feedbackCorrectField.text)
            put("feedback_wrong", feedbackWrongField.text)
            put("is_always_correct", false)
            put("is_html_enabled", true)
        }
        return StepContent(text = htmlEditor.text, source = source)
    }

    override fun addChangeListener(listener: () -> Unit) { changeListeners.add(listener) }
    override val component: JComponent get() = panel

    private fun fireChange() { changeListeners.forEach { it() } }

    private class SimpleDocListener(private val action: () -> Unit) : javax.swing.event.DocumentListener {
        override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = action()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = action()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = action()
    }
}
