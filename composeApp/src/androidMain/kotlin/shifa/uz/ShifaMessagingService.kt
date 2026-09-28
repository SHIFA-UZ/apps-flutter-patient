package shifa.uz

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import shifa.uz.data.notifications.Push
import shifa.uz.utils.PUSH_CHANNEL

/**
 * 13 §3.2, §8.2 (NOTIF-03). A foreground push becomes a tray notification whose tap opens MainActivity with the
 * data; in the background the OS draws it from the manifest's defaults.
 * ponytail: the foreground text is the backend's, like the background's; Flutter localized the foreground one.
 */
class ShifaMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Push.token.value = token
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled() is the check.
    override fun onMessageReceived(message: RemoteMessage) {
        Push.arrived.tryEmit(Unit)
        val data = message.data
        // Tray id = server id (13 §11.8).
        val id = (data["notificationId"] ?: data["id"])?.toIntOrNull() ?: System.currentTimeMillis().toInt()
        val body = message.notification?.body ?: data["message"]
        val tap =
            PendingIntent.getActivity(
                this,
                id,
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .apply { data.forEach { (key, value) -> putExtra(key, value) } },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        manager.notify(
            id,
            NotificationCompat
                .Builder(this, PUSH_CHANNEL)
                .setSmallIcon(R.drawable.splash_logo)
                .setColor(getColor(R.color.ic_launcher_background))
                .setContentTitle(message.notification?.title ?: data["title"])
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(tap)
                .build(),
        )
    }
}
