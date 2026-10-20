package shifa.uz.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

// AppDelegate shows it in the foreground and hands its tap to PushBridge, like a remote push.
@Composable
actual fun rememberLocalPush(): (title: String, body: String, data: Map<String, String>) -> Boolean =
    remember {
        { title, body, data ->
            val content = UNMutableNotificationContent()
            content.setTitle(title)
            content.setBody(body)
            content.setUserInfo(data.mapKeys { it.key as Any? })
            UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
                UNNotificationRequest.requestWithIdentifier(NSUUID().UUIDString, content, null),
                null,
            )
            true
        }
    }
