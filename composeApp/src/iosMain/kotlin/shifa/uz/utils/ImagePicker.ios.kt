package shifa.uz.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(onImageSelected: (ImagePickerResult?) -> Unit): () -> Unit {
    return {
        onImageSelected(null)
    }
}
