package shifa.uz.data.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class TokenStorage(private val settings: Settings = secureSettings) {
    companion object {
        private const val KEY_TOKEN = "auth_token"
    }

    var token: String?
        get() = settings.getStringOrNull(KEY_TOKEN)
        set(value) {
            if (value != null) {
                settings[KEY_TOKEN] = value
            } else {
                settings.remove(KEY_TOKEN)
            }
        }

    fun clear() {
        settings.remove(KEY_TOKEN)
    }

    fun isLoggedIn(): Boolean = token != null
}
