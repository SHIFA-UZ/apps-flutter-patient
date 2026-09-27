package shifa.uz.data.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.COULD_NOT_SEND_EMAIL_OTP
import shifa.uz.data.api.COULD_NOT_SEND_SMS_OTP
import shifa.uz.data.api.COULD_NOT_SEND_VERIFICATION_CODE
import shifa.uz.data.api.COULD_NOT_VERIFY_ACCOUNT
import shifa.uz.data.api.FAILED_TO_CHANGE_PASSWORD
import shifa.uz.data.api.FAILED_TO_CREATE_PATIENT_ACCOUNT
import shifa.uz.data.api.FAILED_TO_RESET_PASSWORD
import shifa.uz.data.api.LOGIN_FAILED
import shifa.uz.data.api.NO_TOKEN_RECEIVED
import shifa.uz.data.api.REGISTRATION_FAILED
import shifa.uz.data.api.SMS_COULD_NOT_BE_SENT
import shifa.uz.data.api.TOO_MANY_REQUESTS
import shifa.uz.data.api.TOO_MANY_VERIFICATION_REQUESTS
import shifa.uz.data.api.apiCall
import shifa.uz.data.api.httpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChangePasswordRequest
import shifa.uz.data.model.CheckIdentifierRequest
import shifa.uz.data.model.CheckIdentifierResponse
import shifa.uz.data.model.CreatePatientForDoctorRequest
import shifa.uz.data.model.FcmTokenRequest
import shifa.uz.data.model.ForgotPasswordOtpRequest
import shifa.uz.data.model.ForgotPasswordOtpResponse
import shifa.uz.data.model.ForgotPasswordResetRequest
import shifa.uz.data.model.LoginRequest
import shifa.uz.data.model.LoginResponse
import shifa.uz.data.model.OtpChannel
import shifa.uz.data.model.RegisterPatientRequest
import shifa.uz.data.model.SendEmailOtpRequest
import shifa.uz.data.model.SendSmsOtpRequest
import shifa.uz.data.model.SendSmsOtpResponse
import shifa.uz.data.model.TokenResponse
import shifa.uz.data.storage.TokenStorage

private const val KEY_HAS_LAUNCHED = "has_launched_before"

class AuthRepository(
    private val client: HttpClient = httpClient,
    private val tokenStorage: TokenStorage = TokenStorage(),
    private val settings: Settings = Settings(),
) {
    /** A1. Saves the token on success. */
    suspend fun login(
        username: String,
        password: String,
    ): Result<LoginResponse> =
        apiCall<LoginResponse>(LOGIN_FAILED) {
            client.post(ApiConfig.Endpoints.LOGIN) {
                parameter("app", "patient")
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }
        }.mapCatching {
            if (it.token.isBlank()) throw ApiException.ServerError(NO_TOKEN_RECEIVED)
            tokenStorage.token = it.token
            it
        }

    /** A2. No account (a 404 or a missing type) is `type = "none"`. */
    suspend fun checkIdentifier(
        phone: String?,
        email: String?,
    ): Result<CheckIdentifierResponse> =
        apiCall<CheckIdentifierResponse?>(COULD_NOT_VERIFY_ACCOUNT) {
            postJson(ApiConfig.Endpoints.CHECK_IDENTIFIER, CheckIdentifierRequest(phone, email))
        }.map { it ?: CheckIdentifierResponse() }
            .recoverCatching { if (it is ApiException.NotFound) CheckIdentifierResponse() else throw it }

    /** A4. [purpose] is `ACCOUNT_DELETION` for the deletion code. The response body is not read. */
    suspend fun sendEmailOtp(
        email: String,
        purpose: String? = null,
    ): Result<Unit> =
        apiCall<String>(COULD_NOT_SEND_EMAIL_OTP) {
            postJson(ApiConfig.Endpoints.SEND_EMAIL_OTP, SendEmailOtpRequest(email, purpose))
        }.map {}

    /** A8, change-password variant. A 401 (wrong current password) is under /auth/, so it doesn't log out. */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit> =
        apiCall<String>(FAILED_TO_CHANGE_PASSWORD) {
            postJson(ApiConfig.Endpoints.RESET_PASSWORD, ChangePasswordRequest(currentPassword, newPassword))
        }.map {}

    /** A5. Where the code went: the server may fall back to email. See [needsEmailFallback] for failures. */
    suspend fun sendSmsOtp(phone: String): Result<OtpChannel> =
        apiCall<SendSmsOtpResponse>(COULD_NOT_SEND_SMS_OTP) {
            postJson(ApiConfig.Endpoints.SEND_SMS_OTP, SendSmsOtpRequest(phone))
        }.map { if (it.channel.equals("email", ignoreCase = true)) OtpChannel.Email else OtpChannel.Sms }

    /** A7. Saves the token on success. */
    suspend fun registerPatient(request: RegisterPatientRequest): Result<Unit> =
        apiCall<TokenResponse>(REGISTRATION_FAILED) {
            postJson(ApiConfig.Endpoints.REGISTER_PATIENT, request)
        }.saveToken(REGISTRATION_FAILED)

    /** A6 (05 §2.5): a patient account for an existing doctor. Saves the token on success. */
    suspend fun createPatientForDoctor(
        phone: String?,
        email: String,
        emailOtp: String,
    ): Result<Unit> =
        apiCall<TokenResponse>(FAILED_TO_CREATE_PATIENT_ACCOUNT) {
            postJson(
                ApiConfig.Endpoints.CREATE_PATIENT_FOR_DOCTOR,
                CreatePatientForDoctorRequest(phone, email, emailOtp),
            )
        }.saveToken(NO_TOKEN_RECEIVED)

    /** A9. [identifier] is a lowercased email or a normalized phone. Returns where the code went. */
    suspend fun sendForgotPasswordOtp(identifier: String): Result<OtpChannel> =
        apiCall<ForgotPasswordOtpResponse>(COULD_NOT_SEND_VERIFICATION_CODE) {
            postJson(ApiConfig.Endpoints.SEND_FORGOT_PASSWORD_OTP, ForgotPasswordOtpRequest(identifier))
        }.map { if (it.channel.equals("sms", ignoreCase = true)) OtpChannel.Sms else OtpChannel.Email }

    /**
     * A10, the only place the code is checked. Saves the token on success (auto-login).
     * The identifier goes under its own key and the code under the channel's, so a phone whose code was
     * emailed is not sent as `email` (fixes 05 Q13; what the backend expects there is unverified).
     */
    suspend fun resetForgottenPassword(
        identifier: String,
        channel: OtpChannel,
        code: String,
        newPassword: String,
    ): Result<Unit> {
        val isEmail = '@' in identifier
        val sms = channel == OtpChannel.Sms
        val request =
            ForgotPasswordResetRequest(
                newPassword = newPassword,
                phone = identifier.takeUnless { isEmail },
                email = identifier.takeIf { isEmail },
                smsOtp = code.takeIf { sms },
                emailOtp = code.takeUnless { sms },
            )
        return apiCall<TokenResponse>(FAILED_TO_RESET_PASSWORD) {
            postJson(ApiConfig.Endpoints.FORGOT_PASSWORD_RESET, request)
        }.saveToken(NO_TOKEN_RECEIVED)
    }

    private suspend inline fun <reified T> postJson(
        path: String,
        body: T,
    ) = client.post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private fun Result<TokenResponse>.saveToken(missingMessage: String): Result<Unit> =
        mapCatching {
            if (it.token.isBlank()) throw ApiException.ServerError(missingMessage)
            tokenStorage.token = it.token
        }

    /**
     * 05 §2.1: true if a stored token is still good. Only a 401 from the profile call rejects it;
     * any other failure (offline, 5xx) keeps the session.
     */
    suspend fun restoreSession(): Boolean =
        withContext(Dispatchers.Default) {
            try {
                // The iOS Keychain survives a reinstall; drop its token on the first launch.
                if (!settings.getBoolean(KEY_HAS_LAUNCHED, false)) {
                    tokenStorage.clear()
                    settings[KEY_HAS_LAUNCHED] = true
                }
                if (tokenStorage.token == null) return@withContext false
                val rejected = apiCall<Unit>("") { client.get(ApiConfig.Endpoints.PROFILE) }.exceptionOrNull()
                if (rejected is ApiException.Unauthorized) tokenStorage.clear()
                rejected !is ApiException.Unauthorized
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }

    /** 05 §2.8: unregisters push (P7 with "", errors ignored), then drops the token. No logout endpoint. */
    suspend fun logout() {
        apiCall<String>("") {
            client.put(ApiConfig.Endpoints.FCM_TOKEN) {
                contentType(ContentType.Application.Json)
                setBody(FcmTokenRequest(""))
            }
        }
        tokenStorage.clear()
    }
}

/** A5 failures that switch registration to the email code (05 §2.4 step 5): SMS unavailable or rate limited. */
fun Throwable.needsEmailFallback() =
    this is ApiException.RateLimit ||
        message in setOf(SMS_COULD_NOT_BE_SENT, TOO_MANY_VERIFICATION_REQUESTS, TOO_MANY_REQUESTS)
