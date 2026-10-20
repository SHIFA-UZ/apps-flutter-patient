package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The pages of a PDF, scrollable; [fallback] when it doesn't open. */
@Composable
expect fun PdfView(
    bytes: ByteArray,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
)
