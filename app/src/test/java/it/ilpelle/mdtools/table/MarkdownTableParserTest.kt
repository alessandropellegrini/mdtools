package it.ilpelle.mdtools.table

import org.junit.Assert.*
import org.junit.Test

class MarkdownTableParserTest {

    @Test
    fun testParseStandardTable() {
        val markdown = """
            | Header 1 | Header 2 | Header 3 |
            | :--- | :---: | ---: |
            | Alpha | Beta | Gamma |
            | One | Two | Three |
        """.trimIndent()

        val table = MarkdownTableParser.parse(markdown)
        assertNotNull(table)
        table!!
        assertEquals(3, table.columnCount)
        assertEquals(2, table.rowCount)

        assertEquals("Header 1", table.getHeader(0))
        assertEquals("Header 2", table.getHeader(1))
        assertEquals("Header 3", table.getHeader(2))

        assertEquals(ColumnAlignment.LEFT, table.getAlignment(0))
        assertEquals(ColumnAlignment.CENTER, table.getAlignment(1))
        assertEquals(ColumnAlignment.RIGHT, table.getAlignment(2))

        assertEquals("Alpha", table.getCell(0, 0))
        assertEquals("Beta", table.getCell(0, 1))
        assertEquals("Gamma", table.getCell(0, 2))

        assertEquals("One", table.getCell(1, 0))
        assertEquals("Two", table.getCell(1, 1))
        assertEquals("Three", table.getCell(1, 2))
    }

    @Test
    fun testParseTableWithoutOuterPipes() {
        val markdown = """
            Fruit | Price | Stock
            :--- | :---: | ---:
            Apple | $1.00 | 100
            Banana | $0.50 | 250
        """.trimIndent()

        val table = MarkdownTableParser.parse(markdown)
        assertNotNull(table)
        table!!
        assertEquals(3, table.columnCount)
        assertEquals(2, table.rowCount)
        assertEquals("Fruit", table.getHeader(0))
        assertEquals("Apple", table.getCell(0, 0))
        assertEquals("$1.00", table.getCell(0, 1))
        assertEquals("100", table.getCell(0, 2))
    }

    @Test
    fun testParseTableWithEscapedPipes() {
        val markdown = """
            | Command | Description |
            | --- | --- |
            | `ls \| grep foo` | Pipes ls into grep |
        """.trimIndent()

        val table = MarkdownTableParser.parse(markdown)
        assertNotNull(table)
        table!!
        assertEquals(2, table.columnCount)
        assertEquals(1, table.rowCount)
        assertEquals("`ls | grep foo`", table.getCell(0, 0))
        assertEquals("Pipes ls into grep", table.getCell(0, 1))
    }

    @Test
    fun testParseTableSurroundedByText() {
        val markdown = """
            # Document Title

            Here is an introductory paragraph explaining data.

            | Name | Role |
            | --- | --- |
            | Alice | Engineer |
            | Bob | Designer |

            And here is a concluding paragraph.
        """.trimIndent()

        val table = MarkdownTableParser.parse(markdown)
        assertNotNull(table)
        table!!
        assertEquals(2, table.columnCount)
        assertEquals(2, table.rowCount)
        assertEquals("Alice", table.getCell(0, 0))
        assertEquals("Bob", table.getCell(1, 0))
    }

    @Test
    fun testParseInvalidTableReturnsNull() {
        val text1 = "Just regular text with no pipes."
        assertNull(MarkdownTableParser.parse(text1))

        val text2 = """
            | Header 1 | Header 2 |
            Just plain text without delimiter row
        """.trimIndent()
        assertNull(MarkdownTableParser.parse(text2))
    }

    @Test
    fun testParseUnevenRows() {
        val markdown = """
            | A | B | C |
            |---|---|---|
            | 1 |
            | 2 | 3 | 4 | 5 |
        """.trimIndent()

        val table = MarkdownTableParser.parse(markdown)
        assertNotNull(table)
        table!!
        assertEquals(3, table.columnCount)
        assertEquals(2, table.rowCount)
        assertEquals("1", table.getCell(0, 0))
        assertEquals("", table.getCell(0, 1))
        assertEquals("", table.getCell(0, 2))
        assertEquals("2", table.getCell(1, 0))
        assertEquals("3", table.getCell(1, 1))
        assertEquals("4", table.getCell(1, 2))
    }
}
