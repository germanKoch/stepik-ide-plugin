package com.stellarflux.stepik.editor

import com.intellij.openapi.project.Project
import com.stellarflux.stepik.editor.forms.ChoiceStepForm
import com.stellarflux.stepik.editor.forms.MatchingStepForm
import com.stellarflux.stepik.editor.forms.StepForm
import com.stellarflux.stepik.editor.forms.StringStepForm
import com.stellarflux.stepik.editor.forms.TextStepForm
import com.stellarflux.stepik.model.StepContent
import com.stellarflux.stepik.model.StepData
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
