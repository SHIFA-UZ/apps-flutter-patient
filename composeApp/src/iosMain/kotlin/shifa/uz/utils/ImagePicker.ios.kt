package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val JPEG_QUALITY = 0.9

// PHPicker needs no photo-library permission. The image is re-encoded as JPEG, so HEIC photos decode and upload.
@Composable
actual fun rememberImagePickerLauncher(onImageSelected: (ImagePickerResult?) -> Unit): () -> Unit {
    val onSelected by rememberUpdatedState(onImageSelected)
    val delegate =
        remember {
            object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>,
                ) {
                    picker.dismissViewControllerAnimated(true, completion = null)
                    val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
                    if (provider == null) return onSelected(null)
                    provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                        val jpeg = data?.let { UIImage(data = it) }?.let { UIImageJPEGRepresentation(it, JPEG_QUALITY) }
                        dispatch_async(dispatch_get_main_queue()) {
                            onSelected(jpeg?.let { ImagePickerResult("", "photo.jpg", it.toByteArray()) })
                        }
                    }
                }
            }
        }
    return {
        val config =
            PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
        val picker = PHPickerViewController(config)
        picker.delegate = delegate
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            picker,
            animated = true,
            completion = null,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).apply {
        if (isNotEmpty()) usePinned { platform.posix.memcpy(it.addressOf(0), bytes, length) }
    }
