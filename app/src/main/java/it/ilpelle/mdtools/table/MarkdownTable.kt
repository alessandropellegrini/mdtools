package it.ilpelle.mdtools.table

enum class ColumnAlignment(val symbol: String, val shortName: String) {
    LEFT("←", "Left"),
    CENTER("↔", "Center"),
    RIGHT("→", "Right"),
    NONE("—", "Default");

    fun next(): ColumnAlignment = when (this) {
        LEFT, NONE -> CENTER
        CENTER -> RIGHT
        RIGHT -> LEFT
    }
}

/**
 * Data model for a Markdown table.
 * Supports dynamic addition/removal of rows and columns, in-place cell editing,
 * and column alignment management.
 */
class MarkdownTable(
    headers: List<String> = emptyList(),
    alignments: List<ColumnAlignment> = emptyList(),
    rows: List<List<String>> = emptyList()
) {
    val headers: MutableList<String> = headers.toMutableList()
    val alignments: MutableList<ColumnAlignment> = alignments.toMutableList()
    val rows: MutableList<MutableList<String>> = rows.map { it.toMutableList() }.toMutableList()

    init {
        // Ensure alignments match header count
        while (this.alignments.size < this.headers.size) {
            this.alignments.add(ColumnAlignment.LEFT)
        }
        if (this.alignments.size > this.headers.size) {
            this.alignments.subList(this.headers.size, this.alignments.size).clear()
        }
        // Normalize rows to match header count
        for (row in this.rows) {
            while (row.size < this.headers.size) {
                row.add("")
            }
            if (row.size > this.headers.size) {
                row.subList(this.headers.size, row.size).clear()
            }
        }
    }

    val columnCount: Int
        get() = headers.size

    val rowCount: Int
        get() = rows.size

    fun addRow(index: Int = rowCount, initialValues: List<String>? = null): Int {
        val targetIndex = index.coerceIn(0, rowCount)
        val newRow = MutableList(columnCount) { col ->
            initialValues?.getOrNull(col) ?: ""
        }
        rows.add(targetIndex, newRow)
        return targetIndex
    }

    fun deleteRow(index: Int): Boolean {
        if (index in 0 until rowCount) {
            rows.removeAt(index)
            return true
        }
        return false
    }

    fun addColumn(
        index: Int = columnCount,
        name: String? = null,
        alignment: ColumnAlignment = ColumnAlignment.LEFT,
        initialValues: List<String>? = null
    ): Int {
        val targetIndex = index.coerceIn(0, columnCount)
        val colName = name?.ifBlank { null } ?: "Col ${columnCount + 1}"
        headers.add(targetIndex, colName)
        alignments.add(targetIndex, alignment)
        for (r in 0 until rowCount) {
            val cellVal = initialValues?.getOrNull(r) ?: ""
            rows[r].add(targetIndex, cellVal)
        }
        return targetIndex
    }

    fun deleteColumn(index: Int): Boolean {
        if (columnCount <= 1) {
            // Keep at least one column
            return false
        }
        if (index in 0 until columnCount) {
            headers.removeAt(index)
            alignments.removeAt(index)
            for (row in rows) {
                if (index in 0 until row.size) {
                    row.removeAt(index)
                }
            }
            return true
        }
        return false
    }

    fun getCell(rowIndex: Int, colIndex: Int): String {
        return if (rowIndex in 0 until rowCount && colIndex in 0 until columnCount) {
            rows[rowIndex][colIndex]
        } else {
            ""
        }
    }

    fun setCell(rowIndex: Int, colIndex: Int, value: String) {
        if (rowIndex in 0 until rowCount && colIndex in 0 until columnCount) {
            rows[rowIndex][colIndex] = value
        }
    }

    fun getHeader(colIndex: Int): String {
        return if (colIndex in 0 until columnCount) headers[colIndex] else ""
    }

    fun setHeader(colIndex: Int, name: String) {
        if (colIndex in 0 until columnCount) {
            headers[colIndex] = name
        }
    }

    fun getAlignment(colIndex: Int): ColumnAlignment {
        return if (colIndex in 0 until columnCount) alignments[colIndex] else ColumnAlignment.LEFT
    }

    fun setAlignment(colIndex: Int, alignment: ColumnAlignment) {
        if (colIndex in 0 until columnCount) {
            alignments[colIndex] = alignment
        }
    }

    fun cycleAlignment(colIndex: Int): ColumnAlignment {
        val next = getAlignment(colIndex).next()
        setAlignment(colIndex, next)
        return next
    }

    fun moveRow(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in 0 until rowCount || toIndex !in 0 until rowCount || fromIndex == toIndex) {
            return false
        }
        val item = rows.removeAt(fromIndex)
        rows.add(toIndex, item)
        return true
    }

    fun moveColumn(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in 0 until columnCount || toIndex !in 0 until columnCount || fromIndex == toIndex) {
            return false
        }
        val header = headers.removeAt(fromIndex)
        headers.add(toIndex, header)

        val align = alignments.removeAt(fromIndex)
        alignments.add(toIndex, align)

        for (row in rows) {
            val cell = row.removeAt(fromIndex)
            row.add(toIndex, cell)
        }
        return true
    }

    fun clear() {
        headers.clear()
        alignments.clear()
        rows.clear()
    }

    fun copy(): MarkdownTable {
        return MarkdownTable(
            headers = headers.toList(),
            alignments = alignments.toList(),
            rows = rows.map { it.toList() }
        )
    }

    companion object {
        fun createDefault(cols: Int = 3, rowCount: Int = 3): MarkdownTable {
            val c = cols.coerceAtLeast(1)
            val r = rowCount.coerceAtLeast(1)
            val headers = List(c) { "Column ${it + 1}" }
            val alignments = List(c) { ColumnAlignment.LEFT }
            val rows = List(r) { List(c) { "" } }
            return MarkdownTable(headers, alignments, rows)
        }
    }
}
