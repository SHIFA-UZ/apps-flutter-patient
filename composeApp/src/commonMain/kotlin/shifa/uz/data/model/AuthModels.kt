package shifa.uz.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

/** A1. A missing token decodes as blank and is rejected by the repository. */
@Serializable
data class LoginResponse(
    val token: String = "",
    val forcePasswordReset: Boolean = false,
)

@Serializable
data class CheckIdentifierRequest(
    val phone: String? = null,
    val email: String? = null,
)

/** A2. A missing type means no account. */
@Serializable
data class CheckIdentifierResponse(
    val type: String = "none",
    val firstName: String? = null,
    val lastName: String? = null,
)

@Serializable
data class SendEmailOtpRequest(
    val email: String,
    val purpose: String? = null,
)

/** A8, the change-password variant. */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class SendSmsOtpRequest(
    val phone: String,
)

@Serializable
data class SendSmsOtpResponse(
    val channel: String = "sms",
)

enum class OtpChannel { Email, Sms }

/** A7. Exactly one of [emailOtp] and [smsOtp], matching the channel the code went to. */
@Serializable
data class RegisterPatientRequest(
    val firstName: String,
    val lastName: String,
    val password: String,
    val phone: String? = null,
    val email: String? = null,
    val emailOtp: String? = null,
    val smsOtp: String? = null,
)

@Serializable
data class CreatePatientForDoctorRequest(
    val phone: String?,
    val email: String,
    val emailOtp: String,
)

@Serializable
data class ForgotPasswordOtpRequest(
    val identifier: String,
    val app: String = "patient",
)

/** A9. The code goes by email unless the server says SMS. */
@Serializable
data class ForgotPasswordOtpResponse(
    val channel: String = "email",
)

/** A10. One of [phone] and [email] (the identifier), one of [smsOtp] and [emailOtp] (the channel). */
@Serializable
data class ForgotPasswordResetRequest(
    val newPassword: String,
    val phone: String? = null,
    val email: String? = null,
    val smsOtp: String? = null,
    val emailOtp: String? = null,
    val app: String = "patient",
)

/** A6, A7, A10. A missing token decodes as blank and is rejected by the repository. */
@Serializable
data class TokenResponse(
    val token: String = "",
)
