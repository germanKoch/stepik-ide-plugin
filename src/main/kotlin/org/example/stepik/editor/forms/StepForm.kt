package org.example.stepik.editor.forms

import org.example.stepik.model.StepContent
import javax.swing.JComponent

interface StepForm {
    fun setContent(content: StepContent)
    fun getContent(): StepContent
    fun addChangeListener(listener: () -> Unit)
    val component: JComponent
}
