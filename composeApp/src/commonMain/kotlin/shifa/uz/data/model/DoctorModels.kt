package shifa.uz.data.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** D2, the keys Home reads (15 §5.3). */
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
}
