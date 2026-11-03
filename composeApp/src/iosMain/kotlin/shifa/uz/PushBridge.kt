package shifa.uz

import shifa.uz.data.notifications.Push

// 13 §3.1-3.3: called from iosApp/AppDelegate.swift as PushBridgeKt.

fun onPushToken(token: String) {
    Push.token.value = token
}

/** [type] is the push's `type` entry, for analytics only. */
fun onPushArrived(type: String?) {
    Push.arrived.tryEmit(type)
}

/** The tapped push's userInfo; its string entries are the FCM data. */
fun onPushOpened(userInfo: Map<Any?, *>) {
    Push.opened.value =
        buildMap {
            userInfo.forEach {
                    (key, value) ->
                if (key is String && value is String) put(key, value)
            }
        }
}
