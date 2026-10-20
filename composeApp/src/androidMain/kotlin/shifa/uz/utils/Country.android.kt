package shifa.uz.utils

import java.text.Collator
import java.util.Locale

internal actual fun platformCountries(): List<Country> =
    Locale.getISOCountries()
        .map { code ->
            val region = Locale.Builder().setRegion(code).build()
            Country(code, region.displayCountry, region.getDisplayCountry(Locale.ENGLISH))
        }.sortedWith(compareBy(Collator.getInstance()) { it.name })
