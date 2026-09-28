package shifa.uz.utils

import androidx.compose.runtime.Composable

/** 13 §11.3-4, once signed in: asks for the notification permission, and on Android creates the push channel. */
@Composable
expect fun PushSetup()
