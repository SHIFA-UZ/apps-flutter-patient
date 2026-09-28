package shifa.uz.data.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** D1/D2 (15 §5.3), the keys the app reads. Ids may come as numbers; the lenient JSON reads them into strings. */
@Serializable
data class Doctor(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String? = null,
    val profession: String? = null,
    val clinic: String? = null,
    val photoUrl: String? = null,
    val averageRating: Double? = null,
    val reviewCount: Int = 0,
    val certificates: List<String> = emptyList(),
    val nextAvailableStartAt: String? = null,
    val address: String? = null, // legacy free text
    val locationStreetAddress: String? = null,
    val street: String? = null,
    val locationCity: String? = null,
    val city: String? = null,
    val locationRegion: String? = null,
    val region: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceKm: Double? = null, // D1 with Near me
    val phone: String? = null,
    val email: String? = null,
    val telegram: String? = null,
    val instagram: String? = null,
    val biography: String? = null,
    val furtherInformation: String? = null,
    val services: List<String> = emptyList(), // legacy
    val serviceItems: List<DoctorService> = emptyList(),
    val supportsOnline: Boolean = false,
    val supportsInPerson: Boolean = false,
) {
    val name get() = fullName?.takeIf { it.isNotBlank() } ?: "$firstName $lastName".trim()

    val isVerified get() = certificates.isNotEmpty()

    /** "4.8(12)", or null without reviews (07). */
    val ratingText: String?
        get() {
            val rating = averageRating?.takeIf { reviewCount > 0 } ?: return null
            val tenths = (rating * 10).roundToInt()
            return "${tenths / 10}.${tenths % 10}($reviewCount)"
        }

    // 07 §4.1 derived getters.
    val offersOnline get() = supportsOnline || serviceItems.isNotEmpty()

    val offersInPerson get() = supportsInPerson || !clinic.isNullOrBlank() || !cityName.isNullOrBlank() || latitude != null

    private val cityName get() = locationCity?.ifBlank { null } ?: city

    /** 07 §4.3: "street, city, region", else the legacy address. */
    val fullAddress: String?
        get() =
            listOf(
                locationStreetAddress?.ifBlank { null } ?: street,
                cityName,
                locationRegion?.ifBlank { null } ?: region,
            )
                .mapNotNull { it?.trim()?.ifEmpty { null } }
                .joinToString(", ")
                .ifEmpty { address?.trim()?.ifEmpty { null } }

    val about get() = biography?.ifBlank { null } ?: furtherInformation?.ifBlank { null }
}

@Serializable
data class DoctorService(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val isFreeConsultation: Boolean = false,
    val prices: List<DoctorPrice> = emptyList(),
)

@Serializable
data class DoctorPrice(
    val amountMinor: Long = 0,
    val currency: String = "",
) {
    /** 07 §4.3 profile price: always 2 decimals, e.g. "150000.00 UZS". */
    val text get() = "${amountMinor / 100}.${(amountMinor % 100).toString().padStart(2, '0')} $currency".trim()
}

/** D3 item (15 §5.4), only the keys booking reads. */
@Serializable
data class DoctorLocation(
    val id: Int = 0,
    val isPrimary: Boolean = false,
)
