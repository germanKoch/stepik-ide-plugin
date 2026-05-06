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

class MatchingStepForm(project: Project) : StepForm {

    private val panel = JPanel(BorderLayout())
    private val changeListeners = mutableListOf<() -> Unit>()

    private val costSpinner = JSpinner(SpinnerNumberModel(0, 0, 1000, 1))
    private val preserveFirstsOrderCheckbox = JCheckBox("Preserve left-column order")

    private val tableModel = DefaultTableModel(arrayOf("First", "Second"), 0)
    private val pairsTable = JTable(tableModel)

    private val htmlEditor: EditorTextField

    init {
        val metaPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JBLabel("Cost:"))
            add(costSpinner)
            add(preserveFirstsOrderCheckbox)
        }

        val tablePanel = JPanel(BorderLayout()).apply {
            add(JBLabel("Matching pairs:"), BorderLayout.NORTH)
            add(JBScrollPane(pairsTable).apply { preferredSize = Dimension(0, 150) }, BorderLayout.CENTER)
            val buttons = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JButton("Add").apply { addActionListener { tableModel.addRow(arrayOf("", "")); fireChange() } })
                add(JButton("Remove").apply {
                    addActionListener {
                        val row = pairsTable.selectedRow
                        if (row >= 0) { tableModel.removeRow(row); fireChange() }
                    }
                })
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
            addSettingsProvider { editor -> editor.settings.apply { isUseSoftWraps = true; isLineNumbersShown = true } }
        }

        panel.add(settingsWrapper, BorderLayout.NORTH)
        panel.add(JBScrollPane(htmlEditor), BorderLayout.CENTER)

        costSpinner.addChangeListener { fireChange() }
        preserveFirstsOrderCheckbox.addActionListener { fireChange() }
        tableModel.addTableModelListener { fireChange() }
        htmlEditor.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) { fireChange() }
        })
    }

    override fun setContent(content: StepContent) {
        htmlEditor.text = content.text
        val src = content.source ?: return
        preserveFirstsOrderCheckbox.isSelected = src["preserve_firsts_order"]?.jsonPrimitive?.booleanOrNull ?: false
        tableModel.rowCount = 0
        src["pairs"]?.jsonArray?.forEach { pair ->
            val p = pair.jsonObject
            tableModel.addRow(arrayOf(
                p["first"]?.jsonPrimitive?.contentOrNull ?: "",
                p["second"]?.jsonPrimitive?.contentOrNull ?: "",
            ))
        }
    }

    override fun getContent(): StepContent {
        val pairs = buildJsonArray {
            for (i in 0 until tableModel.rowCount) {
                add(buildJsonObject {
                    put("first", tableModel.getValueAt(i, 0)?.toString() ?: "")
                    put("second", tableModel.getValueAt(i, 1)?.toString() ?: "")
                })
            }
        }
        val source = buildJsonObject {
            put("pairs", pairs)
            put("preserve_firsts_order", preserveFirstsOrderCheckbox.isSelected)
            put("is_html_enabled", true)
        }
        return StepContent(text = htmlEditor.text, source = source)
    }

    override fun addChangeListener(listener: () -> Unit) { changeListeners.add(listener) }
    override val component: JComponent get() = panel

    private fun fireChange() { changeListeners.forEach { it() } }
}
