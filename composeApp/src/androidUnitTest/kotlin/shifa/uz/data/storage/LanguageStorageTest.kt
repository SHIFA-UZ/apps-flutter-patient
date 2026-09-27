package shifa.uz.data.storage

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import org.junit.Test

class LanguageStorageTest {
    private val settings = MapSettings()

    @Test
    fun `the saved language wins over the system one`() {
        settings.putString("app_language", "ru")

        assertThat(LanguageStorage(settings, "de").language.value).isEqualTo(AppLanguage.Russian)
    }

    @Test
    fun `with nothing saved it follows the system, else English`() {
        assertThat(LanguageStorage(settings, "UZ").language.value).isEqualTo(AppLanguage.Uzbek)
        assertThat(LanguageStorage(settings, "fr").language.value).isEqualTo(AppLanguage.English)
    }

    @Test
    fun `a change is kept for the next start`() {
        LanguageStorage(settings, "en").set(AppLanguage.German)

        assertThat(LanguageStorage(settings, "en").language.value).isEqualTo(AppLanguage.German)
    }
}
