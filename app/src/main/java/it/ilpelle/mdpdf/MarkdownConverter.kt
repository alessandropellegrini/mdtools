package it.ilpelle.mdpdf

import android.widget.Toast
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

class MarkdownConverter {
    private val options = MutableDataSet().apply {
        set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
    }

    private val parser: Parser = Parser.builder(options).build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

    fun renderMarkdownToHtml(markdown: String, css: String): String {
        val document = parser.parse(markdown)
        val bodyHtml = renderer.render(document)

        return """
            <!doctype html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                $css
                </style>
            </head>
            <body>
            $bodyHtml
            </body>
            </html>
        """.trimIndent()
    }
}