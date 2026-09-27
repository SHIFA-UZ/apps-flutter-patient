package shifa.uz.data.storage

import androidx.compose.ui.text.intl.Locale
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 14 §5.4. Display names are not translated. */
enum class AppLanguage(val code: String, val displayName: String) {
    English("en", "English"),
    German("de", "Deutsch"),
    Uzbek("uz", "O'zbek"),
    Russian("ru", "Русский"),
    ;

    companion object {
        /** Null for an unknown code, so a stray server value never forces English (14 §9.3). */
        fun fromCode(code: String?): AppLanguage? =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}

/** The app language (CORE-04): the saved choice, else the system one, else English. */
class LanguageStorage(
    private val settings: Settings = Settings(),
    systemCode: String = Locale.current.language,
) {
    private val _language =
        MutableStateFlow(
            AppLanguage.fromCode(
                settings.getStringOrNull(KEY),
            ) ?: AppLanguage.fromCode(systemCode) ?: AppLanguage.English,
        )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun set(language: AppLanguage) {
        settings[KEY] = language.code
        _language.value = language
    }

    private companion object {
        const val KEY = "app_language"
    }
}

val languageStorage by lazy { LanguageStorage() }
