package it.ilpelle.mdpdf
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream

import android.print.printToPdf

class ShareMarkdownActivity : AppCompatActivity() {

    private lateinit var cssSpinner: Spinner
    private lateinit var prefs: SharedPreferences
    private lateinit var webView: WebView

    private var cssEntries: List<CssEntry> = emptyList()

    private sealed class CssEntry(val displayName: String) {
        class AssetCss(displayName: String, val assetPath: String) : CssEntry(displayName)
        class DocumentCss(displayName: String, val uri: Uri) : CssEntry(displayName)
    }

    private val pickCssFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                prefs.edit { putString(PREF_CSS_TREE_URI, treeUri.toString()) }
                loadCssListAndPopulateSpinner()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_markdown)

        prefs = getSharedPreferences("mdpdf_prefs", MODE_PRIVATE)
        cssSpinner = findViewById(R.id.cssSpinner)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = false
        }

        loadCssListAndPopulateSpinner()

        findViewById<Button>(R.id.btnPickCssFolder).setOnClickListener {
            pickCssFolderLauncher.launch(null)
        }

        findViewById<Button>(R.id.btnConvert).setOnClickListener {
            handleConvert()
        }
    }

    private fun loadCssListAndPopulateSpinner() {
        val list = mutableListOf<CssEntry>()

        // Built-in CSS from assets
        list.add(CssEntry.AssetCss("Classic", "classic.css"))
        list.add(CssEntry.AssetCss("Centred Titles", "centred-titles.css"))
        list.add(CssEntry.AssetCss("Modern Word", "modern.css"))
        list.add(CssEntry.AssetCss("Numbered Sections", "numbered.css"))

        // User CSS from the chosen folder (if any)
        val treeUriString = prefs.getString(PREF_CSS_TREE_URI, null)
        if (treeUriString != null) {
            val treeUri = treeUriString.toUri()
            list.addAll(loadUserCssFromTree(treeUri))
        }

        cssEntries = list

        val labels = cssEntries.map { it.displayName }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cssSpinner.adapter = adapter
    }

    private fun loadUserCssFromTree(treeUri: Uri): List<CssEntry> {
        val result = mutableListOf<CssEntry>()
        val docTree = DocumentFile.fromTreeUri(this, treeUri) ?: return emptyList()

        for (file in docTree.listFiles()) {
            if (file.isFile && (file.name?.endsWith(".css", ignoreCase = true) == true)) {
                result.add(CssEntry.DocumentCss(file.name ?: "css", file.uri))
            }
        }
        return result
    }

    private fun handleConvert() {
        if (cssEntries.isEmpty()) {
            Toast.makeText(this, "No CSS available", Toast.LENGTH_SHORT).show()
            return
        }

        val cssEntry = cssEntries[cssSpinner.selectedItemPosition]
        val css = readCss(cssEntry)

        val markdown = readMarkdownFromShareIntent(intent)
        if (markdown == null) {
            Toast.makeText(this, "No markdown content in share intent", Toast.LENGTH_SHORT).show()
            return
        }

        val fullHtml = MarkdownConverter().renderMarkdownToHtml(markdown, css)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                val pdfFile = File(cacheDir, "mdpdf-tmp.pdf")
                exportToPdfAndOpen(pdfFile)
            }
        }

        webView.loadDataWithBaseURL(
            null,
            fullHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun readCss(entry: CssEntry): String {
        return when (entry) {
            is CssEntry.AssetCss ->
                assets.open(entry.assetPath).bufferedReader().use { it.readText() }
            is CssEntry.DocumentCss ->
                contentResolver.openInputStream(entry.uri)?.bufferedReader()?.use { it.readText() }
                    ?: ""
        }
    }

    private fun readMarkdownFromShareIntent(intent: Intent): String? {
        val type = intent.type ?: return null

        if (Intent.ACTION_SEND != intent.action) return null

        if (type == "text/plain" || type.startsWith("text/")) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrEmpty()) return text
        }

        val uri = intent.getIntentParcelableExtra<Uri>(Intent.EXTRA_STREAM, Uri::class.java)
        if (uri != null) {
            return contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }

        return null
    }

    private fun <T> Intent.getIntentParcelableExtra(name: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelableExtra(name)
        }
    }

    private fun exportToPdfAndOpen(pdfFile: File) {
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(
                PrintAttributes.Resolution(
                    "pdf",
                    "PDF",
                    300,
                    300
                )
            )
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        webView.printToPdf(
            context = this,
            pdfFile = pdfFile,
            printAttributes = printAttributes,
            onFinished = {
                openPdf(pdfFile)
                finish()
            },
            onError = { t ->
                Log.e("ShareMarkdownActivity", "PDF generation failed", t)
                Toast.makeText(this, "Error generating PDF from Markdown", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun openPdf(pdfFile: File) {
        val authority = "${packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(this, authority, pdfFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        // Check that there is at least one PDF viewer
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PREF_CSS_TREE_URI = "css_tree_uri"
    }
}
