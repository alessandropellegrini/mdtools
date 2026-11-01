package it.ilpelle.mdtools

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import it.ilpelle.mdtools.pdf.PdfConverterFragment
import it.ilpelle.mdtools.table.TableEditorFragment

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    private var tableFragment: TableEditorFragment? = null
    private var pdfFragment: PdfConverterFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        try {
            setSupportActionBar(toolbar)
        } catch (e: Exception) {
            toolbar.title = getString(R.string.app_name)
        }

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        val markdownContent = extractMarkdownFromIntent(intent)

        val adapter = MainPagerAdapter(this, markdownContent)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.tab_table_editor)
                    tab.setIcon(R.drawable.ic_table)
                }
                1 -> {
                    tab.text = getString(R.string.tab_pdf_converter)
                    tab.setIcon(R.drawable.ic_pdf)
                }
            }
        }.attach()

        // Default tab: If shared to app (ACTION_SEND), switch directly to PDF Converter tab (1)
        val defaultTab = if (intent?.action == Intent.ACTION_SEND) 1 else 0
        val targetTab = intent?.getIntExtra(EXTRA_TAB, defaultTab) ?: defaultTab
        viewPager.currentItem = targetTab
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val markdown = extractMarkdownFromIntent(intent)
        val defaultTab = if (intent.action == Intent.ACTION_SEND) 1 else 0
        val targetTab = intent.getIntExtra(EXTRA_TAB, defaultTab)
        viewPager.currentItem = targetTab

        if (!markdown.isNullOrEmpty()) {
            pdfFragment?.setMarkdownText(markdown)
            tableFragment?.loadMarkdown(markdown)
        }
    }

    private fun extractMarkdownFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        val explicitMarkdown = intent.getStringExtra(EXTRA_MARKDOWN)
        if (!explicitMarkdown.isNullOrEmpty()) return explicitMarkdown

        if (Intent.ACTION_SEND == intent.action) {
            val type = intent.type ?: ""
            if (type.startsWith("text/") || type == "application/octet-stream" || type == "*/*") {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!text.isNullOrEmpty()) return text
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            if (uri != null) {
                return try {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to read stream from share intent", e)
                    null
                }
            }
        }
        return null
    }

    private inner class MainPagerAdapter(
        activity: AppCompatActivity,
        private val initialMarkdown: String?
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    val frag = TableEditorFragment.newInstance(initialMarkdown)
                    tableFragment = frag
                    frag
                }
                1 -> {
                    val frag = PdfConverterFragment.newInstance(initialMarkdown)
                    pdfFragment = frag
                    frag
                }
                else -> throw IllegalStateException("Invalid position $position")
            }
        }
    }

    companion object {
        const val EXTRA_MARKDOWN = "extra_markdown"
        const val EXTRA_TAB = "extra_tab"
    }
}
