package shifa.uz.utils

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun rememberTextShare(): (text: String) -> Unit =
    { text ->
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            UIActivityViewController(activityItems = listOf(text), applicationActivities = null),
            animated = true,
            completion = null,
        )
    }
