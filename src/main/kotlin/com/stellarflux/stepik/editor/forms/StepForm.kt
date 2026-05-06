package com.stellarflux.stepik.editor.forms

import com.stellarflux.stepik.model.StepContent
import javax.swing.JComponent

interface StepForm {
    fun setContent(content: StepContent)
    fun getContent(): StepContent
    fun addChangeListener(listener: () -> Unit)
    val component: JComponent
}
