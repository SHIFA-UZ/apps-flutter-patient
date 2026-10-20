package shifa.uz.utils

import platform.Foundation.ISOCountryCodes
import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.currentLocale
import platform.Foundation.localizedCompare
import platform.Foundation.localizedStringForCountryCode

internal actual fun platformCountries(): List<Country> {
    val local = NSLocale.currentLocale
    val english = NSLocale(localeIdentifier = "en_US")
    return NSLocale.ISOCountryCodes
        .map { code ->
            code as String
            Country(
                code,
                local.localizedStringForCountryCode(code) ?: code,
                english.localizedStringForCountryCode(code) ?: code,
            )
        }.sortedWith { a, b ->
            // Kotlin strings bridge to NSString at runtime.
            @Suppress("CAST_NEVER_SUCCEEDS")
            (a.name as NSString).localizedCompare(b.name).toInt()
        }
}
