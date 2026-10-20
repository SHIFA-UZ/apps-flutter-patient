package shifa.uz.data.storage

import com.russhwolf.settings.Settings

/** 01 §5: the `shifa_auth_secure` store (Keychain on iOS, EncryptedSharedPreferences on Android). */
internal expect fun createSecureSettings(): Settings

internal val secureSettings: Settings by lazy { createSecureSettings() }
