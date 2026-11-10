package shifa.uz.data.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import shifa.uz.data.storage.TokenStorage

/** 13 §3.1-3.3 (NOTIF-03): what the platform push code hands to the shared app. The signed-in shell reads it. */
object Push {
    /** The FCM token, at launch and on each refresh. Sent with P7 once signed in. */
    val token = MutableStateFlow<String?>(null)

    /** A tapped push's data, kept until the signed-in shell routes it (cold start included). */
    val opened = MutableStateFlow<Map<String, String>?>(null)

    /**
     * A push was tapped (Android's MainActivity, iOS's AppDelegate). Signed out, it is dropped: it was for the
     * account that left, and the next one to sign in must not be taken to it.
     */
    fun open(
        data: Map<String, String>,
        signedIn: Boolean = TokenStorage().isLoggedIn(),
    ) {
        if (signedIn) opened.value = data
    }

    /** A push arrived in the foreground (its `type`, when the payload has one), so the bell's count is stale. */
    val arrived = MutableSharedFlow<String?>(extraBufferCapacity = 1)
}
