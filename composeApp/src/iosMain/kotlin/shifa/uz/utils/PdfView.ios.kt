package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView

// PDFKit scrolls and pinch-zooms by itself.
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PdfView(
    bytes: ByteArray,
    modifier: Modifier,
    fallback: @Composable () -> Unit,
) {
    val document =
        remember(
            bytes,
        ) { runCatching { PDFDocument(data = bytes.toNSData()) }.getOrNull()?.takeIf { it.pageCount > 0u } }
    if (document == null) return fallback()
    UIKitView(
        factory = {
            PDFView().apply {
                autoScales = true
                this.document = document
            }
        },
        modifier = modifier,
    )
}
