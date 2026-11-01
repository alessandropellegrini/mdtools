package it.ilpelle.mdtools.table

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import it.ilpelle.mdtools.R

class TableEditorFragment : Fragment() {

    private lateinit var txtTableDimensions: TextView
    private lateinit var tableLayout: TableLayout
    private lateinit var btnAddRow: Button
    private lateinit var btnPasteTable: Button
    private lateinit var btnNewTable: Button
    private lateinit var btnCopyMarkdown: Button
    private lateinit var btnViewCode: Button
    private lateinit var verticalScroll: ScrollView
    private lateinit var horizontalScroll: HorizontalScrollView

    private var currentTable: MarkdownTable = createSampleTable()

    // Cache of data cell EditTexts by [colIndex] for quick alignment changes
    private val columnCellViews = mutableMapOf<Int, MutableList<EditText>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_table_editor, container, false)

        txtTableDimensions = root.findViewById(R.id.txtTableDimensions)
        tableLayout = root.findViewById(R.id.tableLayout)
        btnAddRow = root.findViewById(R.id.btnAddRow)
        btnPasteTable = root.findViewById(R.id.btnPasteTable)
        btnNewTable = root.findViewById(R.id.btnNewTable)
        btnCopyMarkdown = root.findViewById(R.id.btnCopyMarkdown)
        btnViewCode = root.findViewById(R.id.btnViewCode)
        verticalScroll = root.findViewById(R.id.verticalScroll)
        horizontalScroll = root.findViewById(R.id.horizontalScroll)

        // Check if arguments have initial markdown
        arguments?.getString(ARG_MARKDOWN)?.let { md ->
            MarkdownTableParser.parse(md)?.let { parsed ->
                currentTable = parsed
            }
        }

        setupButtons()
        renderTable()

        return root
    }

    fun loadMarkdown(markdown: String) {
        val parsed = MarkdownTableParser.parse(markdown)
        if (parsed != null) {
            currentTable = parsed
            if (::tableLayout.isInitialized) {
                renderTable()
            }
        }
    }

    private fun setupButtons() {
        btnAddRow.setOnClickListener {
            currentTable.addRow()
            renderTable()
            verticalScroll.post {
                verticalScroll.fullScroll(View.FOCUS_DOWN)
            }
        }

        btnPasteTable.setOnClickListener {
            handlePasteWorkflow()
        }

        btnNewTable.setOnClickListener {
            showNewTableDialog()
        }

        btnCopyMarkdown.setOnClickListener {
            copyMarkdownToClipboard()
        }

        btnViewCode.setOnClickListener {
            showMarkdownPreviewDialog()
        }
    }

    private fun handlePasteWorkflow() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipText = clipboard.primaryClip?.let { clip ->
            if (clip.itemCount > 0) clip.getItemAt(0).text?.toString() else null
        }

        if (!clipText.isNullOrBlank()) {
            val parsed = MarkdownTableParser.parse(clipText)
            if (parsed != null) {
                currentTable = parsed
                renderTable()
                Toast.makeText(
                    context,
                    "Table imported from clipboard (${parsed.columnCount} cols × ${parsed.rowCount} rows)",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // If clipboard doesn't contain a valid table, open the import dialog
        showImportMarkdownDialog(clipText ?: "")
    }

    private fun copyMarkdownToClipboard() {
        val markdown = MarkdownTableFormatter.format(currentTable)
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Markdown Table", markdown)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Markdown table copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun showImportMarkdownDialog(initialText: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_import_markdown, null)
        val editInput = dialogView.findViewById<EditText>(R.id.editMarkdownInput)
        val btnPasteClip = dialogView.findViewById<Button>(R.id.btnDialogPasteClipboard)
        val txtError = dialogView.findViewById<TextView>(R.id.txtImportError)

        if (initialText.isNotBlank()) {
            editInput.setText(initialText)
        }

        btnPasteClip.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.let {
                if (it.itemCount > 0) it.getItemAt(0).text?.toString() else null
            }
            if (!text.isNullOrBlank()) {
                editInput.setText(text)
                txtError.visibility = View.GONE
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.import_btn, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val raw = editInput.text.toString()
            val parsed = MarkdownTableParser.parse(raw)
            if (parsed != null) {
                currentTable = parsed
                renderTable()
                Toast.makeText(
                    context,
                    "Imported ${parsed.columnCount} cols × ${parsed.rowCount} rows",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            } else {
                txtError.text = "No valid Markdown table found. Make sure headers and delimiter rows (e.g. |---|) are present."
                txtError.visibility = View.VISIBLE
            }
        }
    }

    private fun showNewTableDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_table, null)
        val inputCols = dialogView.findViewById<EditText>(R.id.inputCols)
        val inputRows = dialogView.findViewById<EditText>(R.id.inputRows)

        val btn2x2 = dialogView.findViewById<Button>(R.id.btnPreset2x2)
        val btn3x3 = dialogView.findViewById<Button>(R.id.btnPreset3x3)
        val btn4x4 = dialogView.findViewById<Button>(R.id.btnPreset4x4)
        val btn5x5 = dialogView.findViewById<Button>(R.id.btnPreset5x5)

        btn2x2.setOnClickListener { inputCols.setText("2"); inputRows.setText("2") }
        btn3x3.setOnClickListener { inputCols.setText("3"); inputRows.setText("3") }
        btn4x4.setOnClickListener { inputCols.setText("4"); inputRows.setText("4") }
        btn5x5.setOnClickListener { inputCols.setText("5"); inputRows.setText("5") }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.create_table) { _, _ ->
                val cols = inputCols.text.toString().toIntOrNull() ?: 3
                val rows = inputRows.text.toString().toIntOrNull() ?: 3
                currentTable = MarkdownTable.createDefault(cols, rows)
                renderTable()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showMarkdownPreviewDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_preview_markdown, null)
        val txtPreview = dialogView.findViewById<TextView>(R.id.txtMarkdownPreview)
        val markdown = MarkdownTableFormatter.format(currentTable)
        txtPreview.text = markdown

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.copy_to_clipboard) { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Markdown Table", markdown)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Markdown table copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun renderTable() {
        tableLayout.removeAllViews()
        columnCellViews.clear()

        val colCount = currentTable.columnCount
        val rowCount = currentTable.rowCount

        txtTableDimensions.text = "$colCount cols × $rowCount rows"

        // 1. HEADER ROW
        val headerRow = TableRow(requireContext())
        headerRow.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT
        )

        // Top-left corner cell (#)
        val cornerView = TextView(requireContext()).apply {
            text = "#"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            setBackgroundResource(R.drawable.row_header_background)
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            val lp = TableRow.LayoutParams(dpToPx(42), dpToPx(60)).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            layoutParams = lp
        }
        headerRow.addView(cornerView)

        // Columns headers
        for (c in 0 until colCount) {
            columnCellViews[c] = mutableListOf()
            val headerCell = createHeaderCell(c)
            headerRow.addView(headerCell)
        }

        // "+ Column" button at end of header row
        val btnAddCol = Button(requireContext()).apply {
            text = "+ Col"
            textSize = 12f
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val lp = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                dpToPx(60)
            ).apply {
                setMargins(dpToPx(4), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            layoutParams = lp
            setOnClickListener {
                currentTable.addColumn()
                renderTable()
                horizontalScroll.post {
                    horizontalScroll.fullScroll(View.FOCUS_RIGHT)
                }
            }
        }
        headerRow.addView(btnAddCol)

        tableLayout.addView(headerRow)

        // 2. DATA ROWS
        for (r in 0 until rowCount) {
            val dataRow = TableRow(requireContext())
            dataRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            // Left row header (row number + delete button)
            val rowHeaderView = createRowHeaderCell(r)
            dataRow.addView(rowHeaderView)

            // Data cells
            for (c in 0 until colCount) {
                val cellView = createDataCell(r, c)
                columnCellViews[c]?.add(cellView)
                dataRow.addView(cellView)
            }

            tableLayout.addView(dataRow)
        }
    }

    private fun createHeaderCell(colIndex: Int): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.header_cell_background)
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            val lp = TableRow.LayoutParams(
                dpToPx(130),
                dpToPx(60)
            ).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            layoutParams = lp
        }

        // Header text input
        val editHeader = EditText(requireContext()).apply {
            setText(currentTable.getHeader(colIndex))
            textSize = 13f
            setSingleLine(true)
            background = null
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, 0, 0, 0)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            layoutParams = lp
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    currentTable.setHeader(colIndex, s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        container.addView(editHeader)

        // Bottom control row: [Align pill] and [Delete col ×]
        val controlRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
        }

        // Alignment button
        val currentAlign = currentTable.getAlignment(colIndex)
        val btnAlign = TextView(requireContext()).apply {
            text = "${currentAlign.symbol} ${currentAlign.shortName}"
            textSize = 10f
            setTextColor(ContextCompat.getColor(context, R.color.badge_text))
            setBackgroundResource(R.drawable.row_header_background)
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            val lp = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            layoutParams = lp
            setOnClickListener {
                val newAlign = currentTable.cycleAlignment(colIndex)
                text = "${newAlign.symbol} ${newAlign.shortName}"
                updateColumnAlignmentViews(colIndex, newAlign)
            }
        }
        controlRow.addView(btnAlign)

        // Delete column button
        val btnDeleteCol = TextView(requireContext()).apply {
            text = "✕"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.delete_btn_color))
            gravity = Gravity.CENTER
            setPadding(dpToPx(6), dpToPx(2), dpToPx(4), dpToPx(2))
            setOnClickListener {
                if (currentTable.columnCount <= 1) {
                    Toast.makeText(context, "Cannot delete the only column", Toast.LENGTH_SHORT).show()
                } else {
                    currentTable.deleteColumn(colIndex)
                    renderTable()
                }
            }
        }
        controlRow.addView(btnDeleteCol)

        container.addView(controlRow)
        return container
    }

    private fun createRowHeaderCell(rowIndex: Int): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.row_header_background)
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            val lp = TableRow.LayoutParams(dpToPx(42), dpToPx(44)).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            layoutParams = lp

            val txtNum = TextView(context).apply {
                text = "${rowIndex + 1}"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                gravity = Gravity.CENTER
                val numLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                layoutParams = numLp
            }
            addView(txtNum)

            val btnDelete = TextView(context).apply {
                text = "✕"
                textSize = 10f
                setTextColor(ContextCompat.getColor(context, R.color.delete_btn_color))
                gravity = Gravity.CENTER
                setPadding(dpToPx(2), 0, dpToPx(2), 0)
                setOnClickListener {
                    currentTable.deleteRow(rowIndex)
                    renderTable()
                }
            }
            addView(btnDelete)

            // Long click to show row options menu
            setOnLongClickListener {
                showRowOptionsMenu(rowIndex)
                true
            }
        }
    }

    private fun showRowOptionsMenu(rowIndex: Int) {
        val options = arrayOf("Insert row above", "Insert row below", "Delete row")
        AlertDialog.Builder(requireContext())
            .setTitle("Row ${rowIndex + 1}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        currentTable.addRow(rowIndex)
                        renderTable()
                    }
                    1 -> {
                        currentTable.addRow(rowIndex + 1)
                        renderTable()
                    }
                    2 -> {
                        currentTable.deleteRow(rowIndex)
                        renderTable()
                    }
                }
            }
            .show()
    }

    private fun createDataCell(rowIndex: Int, colIndex: Int): EditText {
        val align = currentTable.getAlignment(colIndex)
        val editText = EditText(requireContext()).apply {
            setText(currentTable.getCell(rowIndex, colIndex))
            setBackgroundResource(R.drawable.cell_background)
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            gravity = getGravityForAlignment(align)
            val lp = TableRow.LayoutParams(
                dpToPx(130),
                dpToPx(44)
            ).apply {
                setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            layoutParams = lp

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    currentTable.setCell(rowIndex, colIndex, s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        return editText
    }

    private fun updateColumnAlignmentViews(colIndex: Int, alignment: ColumnAlignment) {
        val targetGravity = getGravityForAlignment(alignment)
        columnCellViews[colIndex]?.forEach { cell ->
            cell.gravity = targetGravity
        }
    }

    private fun getGravityForAlignment(alignment: ColumnAlignment): Int {
        return when (alignment) {
            ColumnAlignment.LEFT, ColumnAlignment.NONE -> Gravity.START or Gravity.CENTER_VERTICAL
            ColumnAlignment.CENTER -> Gravity.CENTER
            ColumnAlignment.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
        }
    }

    private fun dpToPx(dp: Int): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    companion object {
        const val ARG_MARKDOWN = "arg_markdown"

        fun newInstance(markdown: String? = null): TableEditorFragment {
            return TableEditorFragment().apply {
                if (markdown != null) {
                    arguments = Bundle().apply {
                        putString(ARG_MARKDOWN, markdown)
                    }
                }
            }
        }

        private fun createSampleTable(): MarkdownTable {
            return MarkdownTable(
                headers = listOf("Item", "Category", "Price"),
                alignments = listOf(ColumnAlignment.LEFT, ColumnAlignment.CENTER, ColumnAlignment.RIGHT),
                rows = listOf(
                    listOf("Coffee", "Beverage", "$3.50"),
                    listOf("Croissant", "Bakery", "$2.75"),
                    listOf("Sandwich", "Lunch", "$6.00")
                )
            )
        }
    }
}
