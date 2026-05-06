package org.example.stepik

import com.intellij.openapi.fileTypes.FileType
import javax.swing.Icon

class StepikFileType : FileType {

    override fun getName(): String = NAME
    override fun getDescription(): String = "Stepik Course File"
    override fun getDefaultExtension(): String = EXTENSION
    override fun getIcon(): Icon? = null
    override fun isBinary(): Boolean = false

    companion object {
        const val NAME = "Stepik"
        const val EXTENSION = "stepik"

        @JvmStatic
        val INSTANCE = StepikFileType()
    }
}
