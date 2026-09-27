package shifa.uz.utils

import androidx.compose.runtime.Composable

expect class FileSaver() {
    fun saveFile(
        fileName: String,
        fileBytes: ByteArray,
    ): Result<String>
}

@Composable
expect fun rememberFileSaver(): FileSaver
