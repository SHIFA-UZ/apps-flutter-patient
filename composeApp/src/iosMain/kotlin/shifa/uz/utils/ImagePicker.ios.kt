package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
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

private const val CAMERA_JPEG_QUALITY = 0.85

// 11 §9: JPEG 85, redrawn upright so viewers that ignore EXIF don't show it sideways.
@Composable
actual fun rememberCameraLauncher(onPhotoTaken: (ImagePickerResult?) -> Unit): () -> Unit {
    val onTaken by rememberUpdatedState(onPhotoTaken)
    val delegate =
        remember {
            object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
                override fun imagePickerController(
                    picker: UIImagePickerController,
                    didFinishPickingMediaWithInfo: Map<Any?, *>,
                ) {
                    val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                    val jpeg = image?.upright()?.let { UIImageJPEGRepresentation(it, CAMERA_JPEG_QUALITY) }
                    picker.dismissViewControllerAnimated(true) {
                        onTaken(jpeg?.let { ImagePickerResult("", "photo.jpg", it.toByteArray()) })
                    }
                }

                override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                    picker.dismissViewControllerAnimated(true) { onTaken(null) }
                }
            }
        }
    return launch@{
        val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(camera)) return@launch onTaken(null)
        val picker = UIImagePickerController()
        picker.sourceType = camera
        picker.delegate = delegate
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            picker,
            animated = true,
            completion = null,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.upright(): UIImage {
    if (imageOrientation == UIImageOrientation.UIImageOrientationUp) return this
    UIGraphicsBeginImageContextWithOptions(size, false, scale)
    val (width, height) = size.useContents { width to height }
    drawInRect(CGRectMake(0.0, 0.0, width, height))
    val redrawn = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return redrawn ?: this
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).apply {
        if (isNotEmpty()) usePinned { platform.posix.memcpy(it.addressOf(0), bytes, length) }
    }

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData =
    usePinned { NSData.create(bytes = if (isEmpty()) null else it.addressOf(0), length = size.toULong()) }
