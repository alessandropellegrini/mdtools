package it.ilpelle.mdtools.table

/**
 * Parser for GitHub Flavored Markdown (GFM) tables.
 * Robustly handles standard pipe tables, borderless edge pipes,
 * alignment specifications, escaped pipes, and uneven rows.
 */
object MarkdownTableParser {

    private val DELIMITER_CELL_REGEX = Regex("""^:?-+:?$""")

    /**
     * Splits a table row into cells, respecting escaped pipes `\|`.
     * Strips leading and trailing outer pipes if present.
     */
    fun splitTableRow(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return emptyList()

        var start = 0
        if (trimmed.startsWith("|")) {
            start = 1
        }

        var end = trimmed.length
        if (end > start && trimmed.endsWith("|")) {
            // Check that trailing pipe was not escaped
            var backslashCount = 0
            var checkIdx = end - 2
            while (checkIdx >= 0 && trimmed[checkIdx] == '\\') {
                backslashCount++
                checkIdx--
            }
            if (backslashCount % 2 == 0) {
                end -= 1
            }
        }

        val content = if (start < end) trimmed.substring(start, end) else ""
        if (content.isEmpty()) {
            return if (trimmed == "|" || trimmed == "||") listOf("") else emptyList()
        }

        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false

        for (ch in content) {
            if (escaped) {
                if (ch == '|') {
                    current.append('|')
                } else {
                    current.append('\\').append(ch)
                }
                escaped = false
            } else if (ch == '\\') {
                escaped = true
            } else if (ch == '|') {
                cells.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(ch)
            }
        }
        if (escaped) {
            current.append('\\')
        }
        cells.add(current.toString().trim())
        return cells
    }

    /**
     * Checks if a line is a valid table delimiter (separator) row.
     * E.g. `| --- | :---: | ---: |` or `---|:---:|---`
     */
    fun isDelimiterRow(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return false
        val cells = splitTableRow(trimmed)
        if (cells.isEmpty()) return false

        for (cell in cells) {
            if (!cell.matches(DELIMITER_CELL_REGEX) || !cell.contains('-')) {
                return false
            }
        }
        return true
    }

    /**
     * Extracts column alignments from a delimiter row.
     */
    fun parseAlignments(delimiterLine: String): List<ColumnAlignment> {
        val cells = splitTableRow(delimiterLine)
        return cells.map { cell ->
            val startsWithColon = cell.startsWith(":")
            val endsWithColon = cell.endsWith(":")
            when {
                startsWithColon && endsWithColon && cell.length >= 2 -> ColumnAlignment.CENTER
                startsWithColon -> ColumnAlignment.LEFT
                endsWithColon -> ColumnAlignment.RIGHT
                else -> ColumnAlignment.LEFT
            }
        }
    }

    /**
     * Parses a Markdown table from a string input.
     * Searches for a table header + delimiter row + data rows.
     * Returns null if no valid Markdown table structure is found.
     */
    fun parse(markdown: String): MarkdownTable? {
        val lines = markdown.lines()
        if (lines.size < 2) return null

        var sepIndex = -1
        for (i in 1 until lines.size) {
            if (isDelimiterRow(lines[i])) {
                // Check if the previous line has table cells
                val prevLine = lines[i - 1].trim()
                if (prevLine.isNotEmpty() && !isDelimiterRow(prevLine)) {
                    sepIndex = i
                    break
                }
            }
        }

        if (sepIndex == -1) return null

        val headerCells = splitTableRow(lines[sepIndex - 1]).toMutableList()
        val alignments = parseAlignments(lines[sepIndex]).toMutableList()

        if (headerCells.isEmpty() && alignments.isEmpty()) return null

        val colCount = maxOf(headerCells.size, alignments.size)
        while (headerCells.size < colCount) {
            headerCells.add("Col ${headerCells.size + 1}")
        }
        while (alignments.size < colCount) {
            alignments.add(ColumnAlignment.LEFT)
        }

        val rows = mutableListOf<List<String>>()
        for (i in (sepIndex + 1) until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                // Table ends at blank line
                break
            }
            if (isDelimiterRow(line)) {
                // Another delimiter row, end of this table
                break
            }
            val cells = splitTableRow(line).toMutableList()
            // Pad or truncate row to column count
            while (cells.size < colCount) {
                cells.add("")
            }
            if (cells.size > colCount) {
                cells.subList(colCount, cells.size).clear()
            }
            rows.add(cells)
        }

        return MarkdownTable(headerCells, alignments, rows)
    }

    /**
     * Returns true if the markdown contains a valid parseable table.
     */
    fun canParse(markdown: String): Boolean = parse(markdown) != null
}
