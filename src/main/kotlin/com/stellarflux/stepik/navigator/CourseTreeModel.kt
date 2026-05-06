package com.stellarflux.stepik.navigator

import com.stellarflux.stepik.model.LessonData
import com.stellarflux.stepik.model.SectionData
import com.stellarflux.stepik.model.StepData
import com.stellarflux.stepik.model.StepikFileData
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CourseTreeModel {

    var treeModel: DefaultTreeModel = DefaultTreeModel(DefaultMutableTreeNode("No course loaded"))
        private set

    fun rebuildFromData(data: StepikFileData) {
        val root = DefaultMutableTreeNode(data.courseTitle)
        for (section in data.sections.sortedBy { it.position }) {
            val sectionNode = DefaultMutableTreeNode(SectionNodeData(section))
            for (lesson in section.lessons.sortedBy { it.position }) {
                val lessonNode = DefaultMutableTreeNode(LessonNodeData(lesson))
                for (step in lesson.steps.sortedBy { it.position }) {
                    val stepNode = DefaultMutableTreeNode(StepNodeData(step, lesson.id))
                    lessonNode.add(stepNode)
                }
                sectionNode.add(lessonNode)
            }
            root.add(sectionNode)
        }
        treeModel = DefaultTreeModel(root)
    }

    data class SectionNodeData(val section: SectionData) {
        override fun toString(): String = "${section.position}. ${section.title}"
    }

    data class LessonNodeData(val lesson: LessonData) {
        override fun toString(): String = lesson.title
    }

    data class StepNodeData(val step: StepData, val lessonId: Int) {
        override fun toString(): String {
            val prefix = "${step.position}. [${step.type}]"
            val dirty = if (step.isDirty) " *" else ""
            val preview = step.effectiveContent.text.take(50).replace(Regex("<[^>]*>"), "").trim()
            return "$prefix $preview$dirty"
        }
    }
}
