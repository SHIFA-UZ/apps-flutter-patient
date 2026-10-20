package shifa.uz.data.model

import kotlinx.serialization.Serializable

/** P2. Only the non-null keys are sent. */
@Serializable
data class ProfileUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val language: String? = null,
    val photoUrl: String? = null,
    val locationCountry: String? = null,
    val locationStreetAddress: String? = null,
)

/** P1 and P2, the keys the app reads (15 §5.1). */
@Serializable
data class PatientProfile(
    val id: String? = null,
    val fullName: String = "",
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val birthDate: String? = null,
    val gender: String? = null,
    val language: String? = null,
    val photoUrl: String? = null,
    val locationStreetAddress: String? = null,
    val subscriptionTier: String? = null,
    val features: List<String> = emptyList(),
)

/** 09 §7: the feature list decides when present, else the tier (only PRO is shut out). Fails open. */
val PatientProfile?.canUseShifaAi: Boolean
    get() =
        when {
            this == null -> true
            features.isNotEmpty() -> "PATIENT_SHIFA_AI" in features
            else -> !subscriptionTier.equals("PRO", ignoreCase = true)
        }

/** P5. An empty [challengeId] is a failure. */
@Serializable
data class DeleteAccountChallenge(
    val challengeId: String = "",
    val maskedEmail: String? = null,
    val expiresInSeconds: Int = 300,
)

@Serializable
data class DeleteAccountConfirmRequest(
    val challengeId: String,
    val email: String,
    val emailOtp: String,
)

@Serializable
data class FcmTokenRequest(val fcmToken: String)
