package shifa.uz.data.model

import kotlinx.serialization.Serializable

/** P2. Only the non-null keys are sent. */
@Serializable
data class ProfileUpdate(
    val birthDate: String? = null,
    val address: String? = null,
    val locationCountry: String? = null,
    val photoUrl: String? = null,
)

/** P1, the keys Home reads (15 §5.1). */
@Serializable
data class PatientProfile(
    val id: String? = null,
    val fullName: String = "",
    val photoUrl: String? = null,
)
