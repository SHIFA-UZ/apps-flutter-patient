package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun rememberDocumentPickerLauncher(onDocumentSelected: (DocumentPickerResult?) -> Unit): () -> Unit {
    val delegate =
        remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>,
                ) {
                    val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()

                    urls.firstOrNull()?.let { url ->
                        try {
                            val fileName = url.lastPathComponent ?: "document.pdf"
                            val nsData = NSData.dataWithContentsOfURL(url)

                            if (nsData != null) {
                                val byteArray = ByteArray(nsData.length.toInt())
                                nsData.bytes?.let { bytes ->
                                    byteArray.usePinned { pinned ->
                                        platform.posix.memcpy(
                                            pinned.addressOf(0),
                                            bytes,
                                            nsData.length.toULong(),
                                        )
                                    }
                                }

                                onDocumentSelected(
                                    DocumentPickerResult(
                                        uri = url.absoluteString ?: "",
                                        fileName = fileName,
                                        fileBytes = byteArray,
                                    ),
                                )
                            } else {
                                onDocumentSelected(null)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onDocumentSelected(null)
                        }
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onDocumentSelected(null)
                }
            }
        }

    return {
        try {
            val picker =
                UIDocumentPickerViewController(
                    documentTypes = listOf("public.data", "public.content"),
                    inMode = platform.UIKit.UIDocumentPickerMode.UIDocumentPickerModeImport,
                )
            picker.delegate = delegate
            picker.allowsMultipleSelection = false

            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(picker, animated = true, completion = null)
        } catch (e: Exception) {
            e.printStackTrace()
            onDocumentSelected(null)
        }
    }
}
