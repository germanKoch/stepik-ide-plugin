package org.example.stepik.editor

import com.intellij.openapi.project.Project
import org.example.stepik.editor.forms.*
import org.example.stepik.model.StepContent
import org.example.stepik.model.StepData
import javax.swing.JComponent

class StepSourcePanel(project: Project, step: StepData) {

    private val form: StepForm = when (step.type) {
        "choice" -> ChoiceStepForm(project)
        "matching" -> MatchingStepForm(project)
        "string" -> StringStepForm(project)
        else -> TextStepForm(project)
    }

    init {
        form.setContent(step.effectiveContent)
    }

    fun getContent(): StepContent = form.getContent()

    fun addChangeListener(listener: () -> Unit) {
        form.addChangeListener(listener)
    }

    val component: JComponent get() = form.component
}
