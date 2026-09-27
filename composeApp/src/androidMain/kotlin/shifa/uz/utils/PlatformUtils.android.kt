package shifa.uz.utils

import android.os.Build

actual fun isAndroidEmulator(): Boolean {
    return Build.FINGERPRINT.contains("generic") ||
        Build.FINGERPRINT.contains("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        Build.PRODUCT.contains("sdk") ||
        Build.PRODUCT.contains("emulator")
}
