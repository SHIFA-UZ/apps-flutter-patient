package shifa.uz.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.theme.ShifaSpacing
import java.io.File

private const val A4 = 1f / 1.414f

// Pages render lazily at the view's width. ponytail: no pinch zoom here (iOS's PDFKit has it); add transformable if asked.
@Composable
actual fun PdfView(
    bytes: ByteArray,
    modifier: Modifier,
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val renderer =
        remember(bytes) {
            runCatching {
                // PdfRenderer needs a seekable file; the open descriptor keeps it readable after the delete.
                val file = File.createTempFile("document", ".pdf", context.cacheDir)
                file.writeBytes(bytes)
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                file.delete()
                PdfRenderer(fd)
            }.getOrNull()
        }
    DisposableEffect(renderer) { onDispose { renderer?.let { synchronized(it) { it.close() } } } }
    if (renderer == null || renderer.pageCount == 0) return fallback()
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        LazyColumn(
            Modifier.fillMaxSize().background(ShifaColors.Bg.Canvas),
            verticalArrangement = Arrangement.spacedBy(ShifaSpacing.Gap.Sm),
        ) {
            items(renderer.pageCount) { index ->
                val page by produceState<ImageBitmap?>(null, renderer, index, width) {
                    value = withContext(Dispatchers.IO) { runCatching { renderer.render(index, width) }.getOrNull() }
                }
                page?.let { Image(it, null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
                    ?: Box(Modifier.fillMaxWidth().aspectRatio(A4).background(ShifaColors.Bg.Surface))
            }
        }
    }
}

// One page at a time: PdfRenderer allows a single open page, and close() may come from the UI thread.
private fun PdfRenderer.render(
    index: Int,
    width: Int,
): ImageBitmap =
    synchronized(this) {
        openPage(index).use { page ->
            val bitmap = Bitmap.createBitmap(width, width * page.height / page.width, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap.asImageBitmap()
        }
    }
