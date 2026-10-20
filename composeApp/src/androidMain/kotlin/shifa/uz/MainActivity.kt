package shifa.uz

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.messaging.FirebaseMessaging
import shifa.uz.data.notifications.Push
import shifa.uz.data.storage.initSecureSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(android.R.style.Theme_Material_Light_NoActionBar)

        // Light bars always: the app has no dark theme, and auto gave white status icons on white in system dark mode.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)
        initSecureSettings(this)
        // A push tapped while the app was closed; not again on restore or from Recents.
        if (savedInstanceState == null && (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            opened(intent)
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { Push.token.value = it }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        opened(intent)
    }

    /** 13 §3.2: the OS's tray and ShifaMessagingService both put a push's data in the extras. */
    private fun opened(intent: Intent) {
        val extras = intent.extras ?: return
        val data = extras.keySet().mapNotNull { key -> extras.getString(key)?.let { key to it } }.toMap()
        if ("type" in data) Push.opened.value = data
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
