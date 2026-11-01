package it.ilpelle.mdtools.table

import org.junit.Assert.*
import org.junit.Test

class MarkdownTableFormatterTest {

    @Test
    fun testFormatSimpleTable() {
        val table = MarkdownTable(
            headers = listOf("Name", "Age"),
            alignments = listOf(ColumnAlignment.LEFT, ColumnAlignment.RIGHT),
            rows = listOf(
                listOf("Alice", "24"),
                listOf("Bob", "30")
            )
        )

        val formatted = MarkdownTableFormatter.format(table)
        val expected = """
            | Name  | Age |
            | :---- | --: |
            | Alice |  24 |
            | Bob   |  30 |
        """.trimIndent() + "\n"

        assertEquals(expected, formatted)
    }

    @Test
    fun testFormatWithCenterAlignment() {
        val table = MarkdownTable(
            headers = listOf("Status"),
            alignments = listOf(ColumnAlignment.CENTER),
            rows = listOf(
                listOf("OK"),
                listOf("PENDING")
            )
        )

        val formatted = MarkdownTableFormatter.format(table)
        // Width of "PENDING" is 7
        // Header "Status" is 6, centered in 7 -> "Status " or " Status"
        val parsed = MarkdownTableParser.parse(formatted)
        assertNotNull(parsed)
        parsed!!
        assertEquals(ColumnAlignment.CENTER, parsed.getAlignment(0))
        assertEquals("Status", parsed.getHeader(0))
        assertEquals("OK", parsed.getCell(0, 0))
        assertEquals("PENDING", parsed.getCell(1, 0))
    }

    @Test
    fun testRoundTrip() {
        val originalMarkdown = """
            | Product | Category | Price |
            |:--------|:--------:|------:|
            | Keyboard | Hardware | $99.99 |
            | Monitor  | Hardware | $249.00 |
        """.trimIndent()

        val parsed = MarkdownTableParser.parse(originalMarkdown)
        assertNotNull(parsed)

        val reformatted = MarkdownTableFormatter.format(parsed!!)
        val reParsed = MarkdownTableParser.parse(reformatted)
        assertNotNull(reParsed)

        assertEquals(parsed.columnCount, reParsed!!.columnCount)
        assertEquals(parsed.rowCount, reParsed.rowCount)
        for (c in 0 until parsed.columnCount) {
            assertEquals(parsed.getHeader(c), reParsed.getHeader(c))
            assertEquals(parsed.getAlignment(c), reParsed.getAlignment(c))
            for (r in 0 until parsed.rowCount) {
                assertEquals(parsed.getCell(r, c), reParsed.getCell(r, c))
            }
        }
    }

    @Test
    fun testEscapePipeInCell() {
        val table = MarkdownTable(
            headers = listOf("Expr"),
            alignments = listOf(ColumnAlignment.LEFT),
            rows = listOf(listOf("a | b"))
        )

        val formatted = MarkdownTableFormatter.format(table)
        assertTrue(formatted.contains("""a \| b"""))

        val parsed = MarkdownTableParser.parse(formatted)
        assertNotNull(parsed)
        assertEquals("a | b", parsed!!.getCell(0, 0))
    }
}
