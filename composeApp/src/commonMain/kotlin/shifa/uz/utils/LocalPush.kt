package shifa.uz.utils

import androidx.compose.runtime.Composable

/**
 * The debug menu's fake pushes: shows a notification with [data] as its payload, the way a backend push arrives,
 * so tapping it routes like a real one. False when notifications are off.
 */
@Composable
expect fun rememberLocalPush(): (title: String, body: String, data: Map<String, String>) -> Boolean
