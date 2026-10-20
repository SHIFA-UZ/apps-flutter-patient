package shifa.uz.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.jetbrains.compose.resources.stringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.notificationChannelDescription
import shifa.composeapp.generated.resources.notificationChannelName

/** 13 §8.1. Also the manifest's `default_notification_channel_id`. */
internal const val PUSH_CHANNEL = "shifa_patient_channel"

@Composable
actual fun PushSetup() {
    val context = LocalContext.current
    val name = stringResource(Res.string.notificationChannelName)
    val description = stringResource(Res.string.notificationChannelDescription)
    // Re-creating renames it in the app's language.
    LaunchedEffect(name, description) {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat
                .Builder(PUSH_CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(name)
                .setDescription(description)
                .build(),
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                ask.launch(permission)
            }
        }
    }
}
