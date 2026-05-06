package com.stellarflux.stepik

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class StepikFileType : FileType {

    override fun getName(): String = NAME
    override fun getDescription(): String = "Stepik Course File"
    override fun getDefaultExtension(): String = EXTENSION
    override fun getIcon(): Icon = ICON
    override fun isBinary(): Boolean = false

    companion object {
        const val NAME = "Stepik"
        const val EXTENSION = "stepik"
        val ICON: Icon = IconLoader.getIcon("/icons/stepik.svg", StepikFileType::class.java)

        @JvmStatic
        val INSTANCE = StepikFileType()
    }
}
