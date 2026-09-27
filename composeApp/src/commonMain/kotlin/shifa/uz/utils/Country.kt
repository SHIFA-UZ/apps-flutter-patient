package shifa.uz.utils

/** [name] is in the device language, [apiName] in English: P2 `locationCountry` holds names like "Uzbekistan" (14 §3.3). */
data class Country(
    val code: String,
    val name: String,
    val apiName: String,
)

/** Every ISO 3166 country the platform knows, sorted by [Country.name]. */
internal expect fun platformCountries(): List<Country>

// Newer OS versions say "Türkiye"; keep the name the doctor catalog uses (07 appendix C).
private val API_NAMES = mapOf("TR" to "Turkey")

/** There is no country endpoint (15: local catalogs). Uzbekistan first. */
fun countries(): List<Country> =
    platformCountries()
        .map { c -> API_NAMES[c.code]?.let { c.copy(apiName = it) } ?: c }
        .sortedByDescending { it.code == "UZ" }
