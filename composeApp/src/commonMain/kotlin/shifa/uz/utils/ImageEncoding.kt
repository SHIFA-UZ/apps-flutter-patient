package shifa.uz.utils

import androidx.compose.ui.graphics.ImageBitmap

/** PNG bytes of this bitmap (the signature for B6). */
expect fun ImageBitmap.encodePng(): ByteArray
