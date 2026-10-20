package shifa.uz.utils

import androidx.compose.runtime.Composable

data class ImagePickerResult(
    val uri: String,
    val fileName: String,
    val fileBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImagePickerResult

        if (uri != other.uri) return false
        if (fileName != other.fileName) return false
        if (!fileBytes.contentEquals(other.fileBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + fileBytes.contentHashCode()
        return result
    }
}

@Composable
expect fun rememberImagePickerLauncher(onImageSelected: (ImagePickerResult?) -> Unit): () -> Unit

/** A JPEG from the camera, upright. Null when cancelled or there's no camera. */
@Composable
expect fun rememberCameraLauncher(onPhotoTaken: (ImagePickerResult?) -> Unit): () -> Unit
