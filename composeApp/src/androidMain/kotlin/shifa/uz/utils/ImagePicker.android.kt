package shifa.uz.utils

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePickerLauncher(onImageSelected: (ImagePickerResult?) -> Unit): () -> Unit {
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val fileBytes = inputStream?.readBytes()

                    val fileName =
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            cursor.moveToFirst()
                            cursor.getString(nameIndex)
                        } ?: "profile_${System.currentTimeMillis()}.jpg"

                    if (fileBytes != null) {
                        onImageSelected(
                            ImagePickerResult(
                                uri = uri.toString(),
                                fileName = fileName,
                                fileBytes = fileBytes,
                            ),
                        )
                    } else {
                        onImageSelected(null)
                    }

                    inputStream?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    onImageSelected(null)
                }
            } else {
                onImageSelected(null)
            }
        }

    return { launcher.launch("image/*") }
}
