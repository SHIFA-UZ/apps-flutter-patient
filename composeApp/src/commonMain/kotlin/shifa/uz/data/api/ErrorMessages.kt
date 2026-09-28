package shifa.uz.data.api

import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.*
import shifa.uz.data.model.ApiException

// Messages the app produces itself. Like backend messages, they're canonical English keys into MESSAGES.
const val LOGIN_FAILED = "Login failed"
const val NO_TOKEN_RECEIVED = "No token received from server"
const val NETWORK_ERROR = "Network error. Check your internet connection and try again."
const val CONNECTION_TIMED_OUT = "Connection timed out. Check your internet and try again."
const val COULD_NOT_VERIFY_ACCOUNT = "Could not verify account details"
const val COULD_NOT_SEND_EMAIL_OTP = "Could not send verification email. Please try again later."
const val COULD_NOT_SEND_SMS_OTP = "Could not send verification SMS. Please try again later."
const val SMS_COULD_NOT_BE_SENT = "SMS could not be sent. Please use email verification or contact support."
const val TOO_MANY_VERIFICATION_REQUESTS = "Too many verification requests. Please try again later."
const val REGISTRATION_FAILED = "Registration failed"
const val FAILED_TO_CREATE_PATIENT_ACCOUNT = "Failed to create patient account"
const val COULD_NOT_SEND_VERIFICATION_CODE = "Could not send verification code"
const val FAILED_TO_RESET_PASSWORD = "Failed to reset password"
const val FAILED_TO_UPLOAD_PHOTO = "Failed to upload photo"
const val FAILED_TO_UPDATE_PROFILE = "Failed to update profile"
const val FAILED_TO_LOAD_PROFILE = "Failed to load profile"
const val FAILED_TO_CHANGE_PASSWORD = "Failed to change password"
const val FAILED_TO_DELETE_ACCOUNT = "Failed to delete account" // No l10n key: shows as errorSomethingWentWrong.
const val FAILED_TO_LOAD_APPOINTMENTS = "Failed to load appointments"
const val FAILED_TO_LOAD_DOCTOR = "Failed to load doctor"
const val FAILED_TO_LOAD_TASKS = "Failed to load tasks"
const val FAILED_TO_LOAD_CHECK_INS = "Failed to load check-ins" // No l10n key: shows as errorSomethingWentWrong.
const val FAILED_TO_SUBMIT_CHECK_IN = "Failed to submit check-in" // No l10n key: shows as errorSomethingWentWrong.
const val FAILED_TO_LOAD_NOTIFICATIONS = "Error loading notifications"
const val FAILED_TO_MARK_READ = "Failed to mark notifications as read" // No l10n key: shows as errorSomethingWentWrong.
const val FAILED_TO_LOAD_DOCUMENTS = "Failed to load documents" // No l10n key: shows as errorSomethingWentWrong.
const val FAILED_TO_UPLOAD_DOCUMENT = "Failed to upload document"
const val FAILED_TO_DELETE_DOCUMENT = "Failed to delete document"
const val FAILED_TO_LOAD_DOCUMENT = "Failed to load document"
const val FAILED_TO_SEARCH_DOCTORS = "Failed to search doctors" // No l10n key: shows as errorSomethingWentWrong.
const val FAILED_TO_CANCEL_APPOINTMENT = "Error cancelling appointment"
const val FAILED_TO_LOAD_SLOTS = "Error loading slots"
const val FAILED_TO_BOOK_APPOINTMENT = "Failed to book appointment"
const val COPILOT_FAILED = "Co-pilot failed" // No l10n key: shows as errorSomethingWentWrong.
private const val INVALID_CREDENTIALS = "Invalid credentials"
private const val NO_ACCOUNT_FOUND = "No account found"
const val TOO_MANY_REQUESTS = "Too many requests. Please try again later."

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
        // A8, a 400.
        "Current password is incorrect" to Res.string.errorCurrentPasswordIncorrect,
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
        TOO_MANY_VERIFICATION_REQUESTS to Res.string.errorTooManyVerificationRequests,
        SMS_COULD_NOT_BE_SENT to Res.string.errorSmsCouldNotBeSent,
        LOGIN_FAILED to Res.string.errorLoginFailed,
        COULD_NOT_VERIFY_ACCOUNT to Res.string.errorCouldNotVerifyAccountDetails,
        COULD_NOT_SEND_EMAIL_OTP to Res.string.errorFailedToSendEmailCode,
        COULD_NOT_SEND_SMS_OTP to Res.string.errorFailedToSendSmsCode,
        REGISTRATION_FAILED to Res.string.errorRegistrationFailed,
        FAILED_TO_CREATE_PATIENT_ACCOUNT to Res.string.errorFailedToCreatePatientAccount,
        COULD_NOT_SEND_VERIFICATION_CODE to Res.string.errorFailedToSendVerificationCode,
        FAILED_TO_RESET_PASSWORD to Res.string.errorFailedToResetPassword,
        // A9. Flutter translated it but its sanitizer dropped it (05 §4.2).
        "No email address on file. Please contact support." to Res.string.errorNoEmailOnFileContactSupport,
        FAILED_TO_UPLOAD_PHOTO to Res.string.failedToUploadPhoto,
        FAILED_TO_UPDATE_PROFILE to Res.string.failedToUpdateProfile,
        FAILED_TO_CHANGE_PASSWORD to Res.string.errorFailedToChangePassword,
        FAILED_TO_LOAD_DOCTOR to Res.string.failedToLoadDoctor,
        FAILED_TO_LOAD_NOTIFICATIONS to Res.string.errorLoadingNotifications,
        FAILED_TO_UPLOAD_DOCUMENT to Res.string.uploadFailed,
        FAILED_TO_DELETE_DOCUMENT to Res.string.failedToDeleteDocument,
        FAILED_TO_LOAD_DOCUMENT to Res.string.errorOpeningDocument,
        FAILED_TO_CANCEL_APPOINTMENT to Res.string.errorCancellingAppointment,
        FAILED_TO_LOAD_SLOTS to Res.string.errorLoadingSlots,
        FAILED_TO_BOOK_APPOINTMENT to Res.string.failedToBookAppointment,
        // B3, special-cased in 06 §3.1.
        "serviceId is required for video consultation" to Res.string.bookingSelectServiceForVideo,
        // T4 (12 §4.4).
        "Boolean value required" to Res.string.checkInPleaseSelectYesOrNo,
        "Numeric value required" to Res.string.checkInValueRequired,
        "Text value required" to Res.string.checkInValueRequired,
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
    auth: Boolean = true,
): String {
    raw?.trim()?.removePrefix("Login error: ")?.takeIf { it in MESSAGES }?.let { return it }
    // The 401/404 texts are the auth sanitizer's (01 §8): elsewhere a 404 isn't "No account found".
    return when {
        status == 429 -> TOO_MANY_REQUESTS
        !auth -> defaultMessage
        status == 401 -> INVALID_CREDENTIALS
        status == 404 -> NO_ACCOUNT_FOUND
        else -> defaultMessage
    }
}

/** 01 §9.3: the localized text to show for a failure. */
fun Throwable.toUserMessage(): StringResource =
    (this as? ApiException)?.message?.let(MESSAGES::get) ?: Res.string.errorSomethingWentWrong
