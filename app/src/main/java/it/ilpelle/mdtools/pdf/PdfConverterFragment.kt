package it.ilpelle.mdtools.pdf

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import it.ilpelle.mdtools.MarkdownConverter
import it.ilpelle.mdtools.R
import android.print.printToPdf
import java.io.File

class PdfConverterFragment : Fragment() {

    private lateinit var editMarkdown: EditText
    private lateinit var btnPickFile: Button
    private lateinit var btnPasteClip: Button
    private lateinit var cssSpinner: Spinner
    private lateinit var btnPickCssFolder: Button
    private lateinit var btnConvert: Button

    private lateinit var prefs: SharedPreferences
    private var webView: WebView? = null
    private var cssEntries: List<CssEntry> = emptyList()

    private sealed class CssEntry(val displayName: String) {
        class AssetCss(displayName: String, val assetPath: String) : CssEntry(displayName)
        class DocumentCss(displayName: String, val uri: Uri) : CssEntry(displayName)
    }

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    val content = requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (content != null) {
                        editMarkdown.setText(content)
                        Toast.makeText(context, "Loaded file successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val pickCssFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                prefs.edit { putString(PREF_CSS_TREE_URI, treeUri.toString()) }
                loadCssListAndPopulateSpinner()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_pdf_converter, container, false)

        editMarkdown = root.findViewById(R.id.editMarkdownContent)
        btnPickFile = root.findViewById(R.id.btnPickMdFile)
        btnPasteClip = root.findViewById(R.id.btnPasteMarkdownPdf)
        cssSpinner = root.findViewById(R.id.pdfCssSpinner)
        btnPickCssFolder = root.findViewById(R.id.btnPickCssFolderPdf)
        btnConvert = root.findViewById(R.id.btnConvertPdf)

        prefs = requireContext().getSharedPreferences("mdtools_prefs", Context.MODE_PRIVATE)

        loadCssListAndPopulateSpinner()
        setupListeners()

        arguments?.getString(ARG_INITIAL_MARKDOWN)?.let { md ->
            if (md.isNotEmpty()) {
                editMarkdown.setText(md)
            }
        }

        return root
    }

    private fun setupListeners() {
        btnPickFile.setOnClickListener {
            pickFileLauncher.launch(arrayOf("text/*", "text/markdown", "text/x-markdown", "text/plain"))
        }

        btnPasteClip.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.let {
                if (it.itemCount > 0) it.getItemAt(0).text?.toString() else null
            }
            if (!text.isNullOrBlank()) {
                editMarkdown.setText(text)
                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        btnPickCssFolder.setOnClickListener {
            pickCssFolderLauncher.launch(null)
        }

        btnConvert.setOnClickListener {
            handleConvert()
        }
    }

    private fun loadCssListAndPopulateSpinner() {
        val list = mutableListOf<CssEntry>()

        list.add(CssEntry.AssetCss("Classic", "classic.css"))
        list.add(CssEntry.AssetCss("Centred Titles", "centred-titles.css"))
        list.add(CssEntry.AssetCss("Modern Word", "modern.css"))
        list.add(CssEntry.AssetCss("Numbered Sections", "numbered.css"))

        val treeUriString = prefs.getString(PREF_CSS_TREE_URI, null)
        if (treeUriString != null) {
            val treeUri = treeUriString.toUri()
            list.addAll(loadUserCssFromTree(treeUri))
        }

        cssEntries = list
        val labels = cssEntries.map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cssSpinner.adapter = adapter
    }

    private fun loadUserCssFromTree(treeUri: Uri): List<CssEntry> {
        val result = mutableListOf<CssEntry>()
        val docTree = DocumentFile.fromTreeUri(requireContext(), treeUri) ?: return emptyList()

        for (file in docTree.listFiles()) {
            if (file.isFile && (file.name?.endsWith(".css", ignoreCase = true) == true)) {
                result.add(CssEntry.DocumentCss(file.name ?: "css", file.uri))
            }
        }
        return result
    }

    private fun handleConvert() {
        val markdown = editMarkdown.text.toString().trim()
        if (markdown.isEmpty()) {
            Toast.makeText(context, "Please enter or paste Markdown content", Toast.LENGTH_SHORT).show()
            return
        }

        if (cssEntries.isEmpty()) {
            Toast.makeText(context, "No CSS themes available", Toast.LENGTH_SHORT).show()
            return
        }

        val cssEntry = cssEntries[cssSpinner.selectedItemPosition]
        val css = readCss(cssEntry)
        val fullHtml = MarkdownConverter().renderMarkdownToHtml(markdown, css)

        if (webView == null) {
            webView = WebView(requireContext()).apply {
                settings.javaScriptEnabled = false
            }
        }
        val currentWebView = webView!!

        currentWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                val pdfFile = File(requireContext().cacheDir, "mdtools-export.pdf")
                exportToPdfAndOpen(pdfFile)
            }
        }

        currentWebView.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
    }

    private fun readCss(entry: CssEntry): String {
        return try {
            when (entry) {
                is CssEntry.AssetCss ->
                    requireContext().assets.open(entry.assetPath).bufferedReader().use { it.readText() }
                is CssEntry.DocumentCss ->
                    requireContext().contentResolver.openInputStream(entry.uri)?.bufferedReader()?.use { it.readText() } ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun exportToPdfAndOpen(pdfFile: File) {
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "PDF", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        webView?.printToPdf(
            context = requireContext(),
            pdfFile = pdfFile,
            printAttributes = printAttributes,
            onFinished = {
                openPdf(pdfFile)
            },
            onError = { t ->
                Log.e("PdfConverterFragment", "PDF generation failed", t)
                Toast.makeText(context, "Error generating PDF from Markdown", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun openPdf(pdfFile: File) {
        val authority = "${requireContext().packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(requireContext(), authority, pdfFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun setMarkdownText(markdown: String) {
        if (::editMarkdown.isInitialized) {
            editMarkdown.setText(markdown)
        } else {
            arguments = (arguments ?: Bundle()).apply {
                putString(ARG_INITIAL_MARKDOWN, markdown)
            }
        }
    }

    companion object {
        const val ARG_INITIAL_MARKDOWN = "arg_initial_markdown"
        private const val PREF_CSS_TREE_URI = "css_tree_uri"

        fun newInstance(initialMarkdown: String? = null): PdfConverterFragment {
            return PdfConverterFragment().apply {
                if (!initialMarkdown.isNullOrEmpty()) {
                    arguments = Bundle().apply {
                        putString(ARG_INITIAL_MARKDOWN, initialMarkdown)
                    }
                }
            }
        }
    }
}
