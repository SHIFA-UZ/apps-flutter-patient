package shifa.uz.data.api

import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.*
import shifa.uz.data.model.ApiException

// Messages the app produces itself. Like backend messages, they're canonical English keys into MESSAGES.
const val LOGIN_FAILED = "Login failed"
const val NO_TOKEN_RECEIVED = "No token received from server"
const val NETWORK_ERROR = "Network error. Check your internet connection and try again."
const val CONNECTION_TIMED_OUT = "Connection timed out. Check your internet and try again."
private const val INVALID_CREDENTIALS = "Invalid credentials"
private const val NO_ACCOUNT_FOUND = "No account found"
private const val TOO_MANY_REQUESTS = "Too many requests. Please try again later."

// 01 §8 known-safe backend strings (plus the app's own), mapped to their l10n keys (01 §9.2).
// Only these ever reach the UI; add entries as features need them.
private val MESSAGES: Map<String, StringResource> =
    mapOf(
        NO_ACCOUNT_FOUND to Res.string.errorNoAccountFound,
        "No doctor account found" to Res.string.errorNoDoctorAccountFound,
        "No patient account found" to Res.string.errorNoPatientAccountFound,
        "No admin account found" to Res.string.errorNoAdminAccountFound,
        "Doctor profile not found" to Res.string.errorDoctorProfileNotFound,
        "Email OTP required when email is provided" to Res.string.errorEmailOtpRequiredWhenEmailProvided,
        "Email verification code required" to Res.string.errorEmailVerificationCodeRequired,
        "Invalid or expired email verification code" to Res.string.errorInvalidOrExpiredEmailVerificationCode,
        "Invalid or expired verification code" to Res.string.invalidVerificationCode,
        "Invalid phone number" to Res.string.errorInvalidPhoneNumber,
        "Account is disabled" to Res.string.errorAccountIsDisabled,
        "Account is disabled." to Res.string.errorAccountIsDisabled,
        "Not a doctor account" to Res.string.errorNotADoctorAccount,
        "Missing Bearer token" to Res.string.errorMissingBearerToken,
        "Firebase verification not configured" to Res.string.errorFirebaseVerificationNotConfigured,
        "Invalid or expired token" to Res.string.errorInvalidOrExpiredToken,
        "Access restricted to doctors." to Res.string.errorAccessRestrictedToDoctors,
        "Your account has been blocked." to Res.string.errorYourAccountHasBeenBlocked,
        "Invalid key" to Res.string.errorInvalidKey,
        "Key already used" to Res.string.errorKeyAlreadyUsed,
        "Username is required" to Res.string.errorUsernameRequired,
        "Password is required" to Res.string.errorPasswordRequired,
        INVALID_CREDENTIALS to Res.string.errorInvalidCredentials,
        "Account is locked" to Res.string.errorAccountLocked,
        "Invalid credentials - password mismatch" to Res.string.errorInvalidCredentialsPasswordMismatch,
        "You need to create a patient account first. Use Create Account in the patient app and link your doctor account." to
            Res.string.errorCreatePatientAccountFirst,
        "Phone verification not configured" to Res.string.errorPhoneVerificationNotConfigured,
        "Invalid or expired phone verification" to Res.string.errorInvalidOrExpiredPhoneVerification,
        "Phone number not found in verification" to Res.string.errorPhoneNumberNotFoundInVerification,
        "Invalid phone in verification" to Res.string.errorInvalidPhoneInVerification,
        "Phone number does not match verification" to Res.string.errorPhoneNumberDoesNotMatchVerification,
        "Email already registered" to Res.string.errorEmailAlreadyRegistered,
        "Phone already registered" to Res.string.errorPhoneAlreadyRegistered,
        "Patient with this phone number already exists." to Res.string.errorPatientWithPhoneAlreadyExists,
        "Phone number not found" to Res.string.errorPhoneNumberNotFound,
        "User not found" to Res.string.errorUserNotFound,
        TOO_MANY_REQUESTS to Res.string.errorTooManyRequests,
        "Too many verification requests. Please try again later." to Res.string.errorTooManyVerificationRequests,
        "SMS could not be sent. Please use email verification or contact support." to Res.string.errorSmsCouldNotBeSent,
        LOGIN_FAILED to Res.string.errorLoginFailed,
        NO_TOKEN_RECEIVED to Res.string.errorNoTokenReceivedFromServer,
        NETWORK_ERROR to Res.string.errorNetworkConnection,
        CONNECTION_TIMED_OUT to Res.string.errorConnectionTimedOut,
    )

/**
 * 01 §8: a known-safe backend message passes through (a `Login error: ` prefix is unwrapped).
 * Anything else, technical or not, is replaced by a status default or [defaultMessage].
 */
fun sanitizeError(
    status: Int,
    raw: String?,
    defaultMessage: String,
): String {
    raw?.trim()?.removePrefix("Login error: ")?.takeIf { it in MESSAGES }?.let { return it }
    return when (status) {
        401 -> INVALID_CREDENTIALS
        404 -> NO_ACCOUNT_FOUND
        429 -> TOO_MANY_REQUESTS
        else -> defaultMessage
    }
}

/** 01 §9.3: the localized text to show for a failure. */
fun Throwable.toUserMessage(): StringResource =
    (this as? ApiException)?.message?.let(MESSAGES::get) ?: Res.string.errorSomethingWentWrong
