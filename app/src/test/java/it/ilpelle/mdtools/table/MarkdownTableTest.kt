package it.ilpelle.mdtools.table

import org.junit.Assert.*
import org.junit.Test

class MarkdownTableTest {

    @Test
    fun testTableCreationAndDefaults() {
        val table = MarkdownTable.createDefault(cols = 3, rowCount = 2)
        assertEquals(3, table.columnCount)
        assertEquals(2, table.rowCount)
        assertEquals("Column 1", table.getHeader(0))
        assertEquals("Column 2", table.getHeader(1))
        assertEquals("Column 3", table.getHeader(2))
        assertEquals("", table.getCell(0, 0))
    }

    @Test
    fun testAddRow() {
        val table = MarkdownTable.createDefault(cols = 2, rowCount = 1)
        table.setCell(0, 0, "A1")
        table.setCell(0, 1, "B1")

        val addedIndex = table.addRow(initialValues = listOf("A2", "B2"))
        assertEquals(2, table.rowCount)
        assertEquals(1, addedIndex)
        assertEquals("A2", table.getCell(1, 0))
        assertEquals("B2", table.getCell(1, 1))

        // Insert at index 0
        table.addRow(0, listOf("A0", "B0"))
        assertEquals(3, table.rowCount)
        assertEquals("A0", table.getCell(0, 0))
        assertEquals("A1", table.getCell(1, 0))
    }

    @Test
    fun testDeleteRow() {
        val table = MarkdownTable.createDefault(cols = 2, rowCount = 3)
        table.setCell(0, 0, "Row0")
        table.setCell(1, 0, "Row1")
        table.setCell(2, 0, "Row2")

        assertTrue(table.deleteRow(1))
        assertEquals(2, table.rowCount)
        assertEquals("Row0", table.getCell(0, 0))
        assertEquals("Row2", table.getCell(1, 0))

        assertFalse(table.deleteRow(99))
    }

    @Test
    fun testAddColumn() {
        val table = MarkdownTable.createDefault(cols = 2, rowCount = 2)
        table.setCell(0, 0, "00")
        table.setCell(0, 1, "01")
        table.setCell(1, 0, "10")
        table.setCell(1, 1, "11")

        table.addColumn(name = "Col 3", alignment = ColumnAlignment.RIGHT, initialValues = listOf("02", "12"))
        assertEquals(3, table.columnCount)
        assertEquals("Col 3", table.getHeader(2))
        assertEquals(ColumnAlignment.RIGHT, table.getAlignment(2))
        assertEquals("02", table.getCell(0, 2))
        assertEquals("12", table.getCell(1, 2))
    }

    @Test
    fun testDeleteColumn() {
        val table = MarkdownTable.createDefault(cols = 3, rowCount = 2)
        table.setHeader(0, "C0")
        table.setHeader(1, "C1")
        table.setHeader(2, "C2")
        table.setCell(0, 0, "v00")
        table.setCell(0, 1, "v01")
        table.setCell(0, 2, "v02")

        assertTrue(table.deleteColumn(1))
        assertEquals(2, table.columnCount)
        assertEquals("C0", table.getHeader(0))
        assertEquals("C2", table.getHeader(1))
        assertEquals("v00", table.getCell(0, 0))
        assertEquals("v02", table.getCell(0, 1))

        // Deleting down to 1 col allowed, but cannot delete the last column
        assertTrue(table.deleteColumn(1))
        assertEquals(1, table.columnCount)
        assertFalse(table.deleteColumn(0)) // Refuses to delete last column
    }

    @Test
    fun testCycleAlignment() {
        val table = MarkdownTable.createDefault(cols = 1, rowCount = 1)
        assertEquals(ColumnAlignment.LEFT, table.getAlignment(0))

        val next1 = table.cycleAlignment(0)
        assertEquals(ColumnAlignment.CENTER, next1)
        assertEquals(ColumnAlignment.CENTER, table.getAlignment(0))

        val next2 = table.cycleAlignment(0)
        assertEquals(ColumnAlignment.RIGHT, next2)

        val next3 = table.cycleAlignment(0)
        assertEquals(ColumnAlignment.LEFT, next3)
    }

    @Test
    fun testMoveRowAndColumn() {
        val table = MarkdownTable.createDefault(cols = 3, rowCount = 3)
        table.setHeader(0, "H0")
        table.setHeader(1, "H1")
        table.setHeader(2, "H2")
        table.setCell(0, 0, "R0")
        table.setCell(1, 0, "R1")
        table.setCell(2, 0, "R2")

        assertTrue(table.moveRow(0, 2))
        assertEquals("R1", table.getCell(0, 0))
        assertEquals("R2", table.getCell(1, 0))
        assertEquals("R0", table.getCell(2, 0))

        assertTrue(table.moveColumn(0, 2))
        assertEquals("H1", table.getHeader(0))
        assertEquals("H2", table.getHeader(1))
        assertEquals("H0", table.getHeader(2))
    }
}
