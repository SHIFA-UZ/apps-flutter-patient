package shifa.uz.utils

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

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

private const val CAMERA_JPEG_QUALITY = 85
private const val CAMERA_MAX_SIDE = 4096

// 11 §9: TakePicture into the cache, then JPEG 85, upright. The manifest declares CAMERA for video calls, and then
// TakePicture throws unless it's granted, so it's asked for first; a denial is a cancel.
@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (ImagePickerResult?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onTaken by rememberUpdatedState(onPhotoTaken)
    val file = remember { File(context.cacheDir, "camera/photo.jpg") }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
            if (!taken) return@rememberLauncherForActivityResult onTaken(null)
            scope.launch {
                val jpeg = withContext(Dispatchers.IO) { runCatching { uprightJpeg(file) }.getOrNull() }
                onTaken(jpeg?.let { ImagePickerResult(Uri.fromFile(file).toString(), "photo.jpg", it) })
            }
        }
    val takePicture = {
        try {
            file.parentFile?.mkdirs()
            launcher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        } catch (e: ActivityNotFoundException) {
            onTaken(null)
        }
    }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) takePicture() else onTaken(null)
        }
    return {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takePicture()
        } else {
            permission.launch(Manifest.permission.CAMERA)
        }
    }
}

// Camera apps often save sideways with an EXIF rotation, which BitmapFactory ignores.
private fun uprightJpeg(file: File): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > CAMERA_MAX_SIDE) sample *= 2
    val bitmap =
        BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
            },
        ) ?: error("Not an image")
    val degrees =
        when (
            ExifInterface(
                file.path,
            ).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    val upright =
        if (degrees == 0f) {
            bitmap
        } else {
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                Matrix().apply {
                    postRotate(degrees)
                },
                true,
            )
        }
    return ByteArrayOutputStream().use {
        upright.compress(Bitmap.CompressFormat.JPEG, CAMERA_JPEG_QUALITY, it)
        it.toByteArray()
    }
}
