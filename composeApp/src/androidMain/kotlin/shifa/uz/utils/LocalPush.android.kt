package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import shifa.uz.showPush

@Composable
actual fun rememberLocalPush(): (title: String, body: String, data: Map<String, String>) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { title, body, data -> context.showPush(System.currentTimeMillis().toInt(), title, body, data) }
    }
}
