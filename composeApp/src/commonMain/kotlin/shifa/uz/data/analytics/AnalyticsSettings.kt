package shifa.uz.data.analytics

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/** Profile → "Share usage data", off means opted out (privacy.md §4). Plain Settings: it isn't a secret. */
class AnalyticsSettings(
    private val settings: Settings = Settings(),
) {
    var optedOut: Boolean
        get() = settings.getBoolean(KEY, false)
        set(value) {
            settings[KEY] = value
        }

    private companion object {
        const val KEY = "analytics_opt_out"
    }
}
