package shifa.uz.utils

import androidx.compose.runtime.Composable

data class DocumentPickerResult(
    val uri: String,
    val fileName: String,
    val fileBytes: ByteArray,
)

@Composable
expect fun rememberDocumentPickerLauncher(onDocumentSelected: (DocumentPickerResult?) -> Unit): () -> Unit
