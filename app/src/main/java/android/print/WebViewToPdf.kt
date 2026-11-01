package android.print

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import java.io.File

fun WebView.printToPdf(
    context: Context,
    pdfFile: File,
    printAttributes: PrintAttributes,
    onFinished: () -> Unit,
    onError: (Throwable) -> Unit = {}
) {
    val adapter = createPrintDocumentAdapter("mdpdf-job")

    val pdfUri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    val pfd: ParcelFileDescriptor =
        context.contentResolver.openFileDescriptor(pdfUri, "w") ?: return

    adapter.onLayout(
        null,
        printAttributes,
        CancellationSignal(),
        object : PrintDocumentAdapter.LayoutResultCallback() {
            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                adapter.onWrite(
                    arrayOf(PageRange.ALL_PAGES),
                    pfd,
                    CancellationSignal(),
                    object : PrintDocumentAdapter.WriteResultCallback() {
                        override fun onWriteFinished(pages: Array<out PageRange>) {
                            try {
                                pfd.close()
                            } catch (t: Throwable) {
                                onError(t)
                            }
                            onFinished()
                        }

                        override fun onWriteFailed(error: CharSequence?) {
                            try {
                                pfd.close()
                            } catch (_: Throwable) { }
                            onError(IllegalStateException(error?.toString() ?: "Write failed"))
                        }
                    }
                )
            }
        },
        Bundle()
    )
}
