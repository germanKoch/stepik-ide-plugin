package com.stellarflux.stepik.editor

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class StepPreviewPanel : Disposable {

    private val panel = JPanel(BorderLayout())
    private var browser: JBCefBrowser? = null

    init {
        if (JBCefApp.isSupported()) {
            browser = JBCefBrowser()
            panel.add(browser!!.component, BorderLayout.CENTER)
        } else {
            panel.add(
                JBLabel("JCEF is not available. Preview is disabled.", SwingConstants.CENTER),
                BorderLayout.CENTER,
            )
        }
    }

    fun updatePreview(html: String) {
        browser?.loadHTML(wrapHtml(html))
    }

    val component: JComponent get() = panel

    override fun dispose() {
        browser?.dispose()
    }

    private fun wrapHtml(content: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                       padding: 16px; margin: 0; line-height: 1.6; color: #333; background: #fff; }
                code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
                pre { background: #f4f4f4; padding: 12px; border-radius: 6px; overflow-x: auto; }
                img { max-width: 100%; height: auto; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
            </style>
        </head>
        <body>$content</body>
        </html>
    """.trimIndent()
}
