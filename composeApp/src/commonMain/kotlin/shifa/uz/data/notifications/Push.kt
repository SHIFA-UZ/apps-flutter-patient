package shifa.uz.data.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** 13 §3.1-3.3 (NOTIF-03): what the platform push code hands to the shared app. The signed-in shell reads it. */
object Push {
    /** The FCM token, at launch and on each refresh. Sent with P7 once signed in. */
    val token = MutableStateFlow<String?>(null)

    /** A tapped push's data, kept until the signed-in shell routes it (cold start included). */
    val opened = MutableStateFlow<Map<String, String>?>(null)

    /** A push arrived in the foreground, so the bell's count is stale. */
    val arrived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}
