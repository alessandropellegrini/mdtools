package it.ilpelle.mdtools.table

/**
 * Formats a [MarkdownTable] into standard GitHub Flavored Markdown (GFM) table syntax.
 * Automatically aligns column widths and outputs proper delimiter rows with alignment markers.
 */
object MarkdownTableFormatter {

    /**
     * Escapes characters in a cell that would break table syntax.
     * Escapes unescaped '|' as '\|' and converts newlines to '<br>'.
     */
    fun escapeCell(cell: String): String {
        val sanitized = cell.replace("\r\n", "<br>").replace("\n", "<br>")
        val sb = StringBuilder()
        var escaped = false
        for (ch in sanitized) {
            if (escaped) {
                sb.append(ch)
                escaped = false
            } else if (ch == '\\') {
                sb.append('\\')
                escaped = true
            } else if (ch == '|') {
                sb.append("""\|""")
            } else {
                sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Formats the table into a clean, aligned Markdown table string.
     */
    fun format(table: MarkdownTable): String {
        if (table.columnCount == 0) return ""

        val colCount = table.columnCount
        val colWidths = IntArray(colCount) { 3 }

        // Compute column widths
        for (c in 0 until colCount) {
            val headerText = escapeCell(table.getHeader(c))
            colWidths[c] = maxOf(colWidths[c], headerText.length)
            for (r in 0 until table.rowCount) {
                val cellText = escapeCell(table.getCell(r, c))
                colWidths[c] = maxOf(colWidths[c], cellText.length)
            }
        }

        val sb = StringBuilder()

        // 1. Header row
        sb.append("| ")
        for (c in 0 until colCount) {
            val text = escapeCell(table.getHeader(c))
            sb.append(padCell(text, colWidths[c], table.getAlignment(c)))
            if (c < colCount - 1) sb.append(" | ")
        }
        sb.append(" |\n")

        // 2. Delimiter row
        sb.append("| ")
        for (c in 0 until colCount) {
            val align = table.getAlignment(c)
            val w = colWidths[c]
            val sep = when (align) {
                ColumnAlignment.CENTER -> {
                    val dashes = maxOf(1, w - 2)
                    ":" + "-".repeat(dashes) + ":"
                }
                ColumnAlignment.RIGHT -> {
                    val dashes = maxOf(1, w - 1)
                    "-".repeat(dashes) + ":"
                }
                ColumnAlignment.LEFT -> {
                    val dashes = maxOf(1, w - 1)
                    ":" + "-".repeat(dashes)
                }
                ColumnAlignment.NONE -> {
                    "-".repeat(w)
                }
            }
            sb.append(sep)
            if (c < colCount - 1) sb.append(" | ")
        }
        sb.append(" |\n")

        // 3. Data rows
        for (r in 0 until table.rowCount) {
            sb.append("| ")
            for (c in 0 until colCount) {
                val text = escapeCell(table.getCell(r, c))
                sb.append(padCell(text, colWidths[c], table.getAlignment(c)))
                if (c < colCount - 1) sb.append(" | ")
            }
            sb.append(" |\n")
        }

        return sb.toString()
    }

    private fun padCell(text: String, width: Int, alignment: ColumnAlignment): String {
        if (text.length >= width) return text
        val remaining = width - text.length
        return when (alignment) {
            ColumnAlignment.RIGHT -> " ".repeat(remaining) + text
            ColumnAlignment.CENTER -> {
                val left = remaining / 2
                val right = remaining - left
                " ".repeat(left) + text + " ".repeat(right)
            }
            ColumnAlignment.LEFT, ColumnAlignment.NONE -> text + " ".repeat(remaining)
        }
    }
}
