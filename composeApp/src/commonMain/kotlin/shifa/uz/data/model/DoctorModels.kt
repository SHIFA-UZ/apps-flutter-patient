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

    val offersInPerson get() =
        supportsInPerson || !clinic.isNullOrBlank() || !cityName.isNullOrBlank() || latitude != null

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

/**
 * D2 `serviceItems[]` (MOBILE_BOOKING_PAYMENT §1): free, pay now (online, Click / Stripe) or pay later (at the
 * clinic). `isFreeConsultation` wins over [paymentType]; a priced service without one is pay now (the backend default).
 */
@Serializable
data class DoctorService(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val isFreeConsultation: Boolean = false,
    val prices: List<DoctorPrice> = emptyList(),
    val paymentType: String? = null, // PAY_NOW, PAY_LATER
    val videoAllowed: Boolean? = null,
) {
    // B3 rejects a video booking whose service has only clinic prices ("Selected service has no price configured").
    val isOnline get() = isFreeConsultation || prices.any { it.locationId == null }

    val isPayLater get() = !isFreeConsultation && paymentType.equals(PAY_LATER, ignoreCase = true)

    val isPayNow get() = !isFreeConsultation && !isPayLater

    // Pay later can't be video (§1); older backends don't send videoAllowed.
    val canVideo get() = (videoAllowed ?: true) && !isPayLater && isOnline

    /** In person at [locationId] needs a price there or one for any clinic; free needs none. */
    fun bookableAt(locationId: Int?) =
        isFreeConsultation || prices.isEmpty() || prices.any { it.locationId == null || it.locationId == locationId }

    /** The price for [locationId] (null: video), else the one for any clinic. */
    fun price(locationId: Int?) =
        prices.firstOrNull { it.locationId == locationId && locationId != null }
            ?: prices.firstOrNull { it.locationId == null }

    private companion object {
        const val PAY_LATER = "PAY_LATER"
    }
}

@Serializable
data class DoctorPrice(
    val amountMinor: Long = 0,
    val currency: String = "",
    val locationId: Int? = null, // the clinic this price is for; null: any, video included
) {
    val text get() = moneyText(amountMinor, currency)
}

/**
 * Figma's "300000.00 UZS": always 2 decimals. Every `amountMinor` is in hundredths of the currency, UZS included
 * (tiyin): the doctor app stores a typed price × 100, Click is charged `amountMinor / 100` so'm and Stripe gets
 * `amountMinor` as its unit amount (15 §1.6). Until 2026-10-31 UZS was shown ×100 too large here, while the
 * provider charged the right amount.
 */
fun moneyText(
    amountMinor: Long,
    currency: String,
): String = "${amountMinor / 100}.${(amountMinor % 100).toString().padStart(2, '0')} $currency".trim()

/** D3 item (15 §5.4), only the keys booking reads. */
@Serializable
data class DoctorLocation(
    val id: Int = 0,
    val isPrimary: Boolean = false,
    val label: String? = null,
    val clinic: String? = null,
    val address: String? = null,
) {
    /** Select Clinic's row: the clinic, else the label, else the address. */
    val name get() = listOf(clinic, label, address).firstNotNullOfOrNull { it?.trim()?.ifEmpty { null } }
}

/** D4 item and D5's reply (15 §5.5). B7 sends only rating, comment and createdAt. */
@Serializable
data class Review(
    val id: String = "",
    val patientName: String? = null,
    val patientPhotoUrl: String? = null,
    val rating: Int = 0,
    val comment: String? = null,
    val createdAt: String? = null,
)

/** D5 body. */
@Serializable
data class ReviewRequest(
    val rating: Int,
    val comment: String?,
    val appointmentId: Long? = null,
)
