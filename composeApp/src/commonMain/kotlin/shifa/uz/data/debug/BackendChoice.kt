package shifa.uz.data.debug

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import shifa.uz.data.api.ApiConfig
import shifa.uz.utils.isDebugBuild

/** The backends a debug build can talk to (15 §1.1). Release builds always use [Production]. */
enum class Backend(
    val baseUrl: String,
) {
    Production(ApiConfig.BASE_URL),
    Development(ApiConfig.DEV_BASE_URL),
}

/** Debug menu → Development backend. Kept across launches; ignored by release builds. */
object BackendChoice {
    private const val KEY = "debug_backend"
    private val settings by lazy { Settings() }
    private val _current by lazy { MutableStateFlow(if (isDebugBuild) saved() else Backend.Production) }
    val current: StateFlow<Backend> get() = _current

    private fun saved() = Backend.entries.firstOrNull { it.name == settings.getStringOrNull(KEY) } ?: Backend.Production

    fun set(backend: Backend) {
        settings[KEY] = backend.name
        _current.value = if (isDebugBuild) backend else Backend.Production
    }
}
