package shifa.uz.data.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
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
import kotlinx.coroutines.withTimeoutOrNull
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
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
import shifa.uz.data.notifications.Push
import shifa.uz.data.storage.CopilotStorage
import shifa.uz.data.storage.TokenStorage

private const val KEY_HAS_LAUNCHED = "has_launched_before"

class AuthRepository(
    private val client: HttpClient = httpClient,
    private val tokenStorage: TokenStorage = TokenStorage(),
    private val settings: Settings = Settings(),
    private val copilot: CopilotStorage = CopilotStorage(),
    private val resetAnalytics: () -> Unit = Analytics::reset,
    private val analytics: AnalyticsTracker = Analytics,
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
    suspend fun sendSmsOtp(phone: String): Result<OtpChannel> = sendSmsOtp(phone, null)

    /** A5 with a [purpose]: `ACCOUNT_DELETION` for the deletion code of an account without an e-mail. */
    suspend fun sendSmsOtp(
        phone: String,
        purpose: String?,
    ): Result<OtpChannel> =
        apiCall<SendSmsOtpResponse>(COULD_NOT_SEND_SMS_OTP) {
            postJson(ApiConfig.Endpoints.SEND_SMS_OTP, SendSmsOtpRequest(phone, purpose))
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
     * The backend reads two pairs only: `email` + `emailOtp` (its `email` may hold a phone, when the code for a
     * phone identifier went to the account's email) and `phone` + `smsOtp`. So the channel picks the keys.
     */
    suspend fun resetForgottenPassword(
        identifier: String,
        channel: OtpChannel,
        code: String,
        newPassword: String,
    ): Result<Unit> {
        val sms = channel == OtpChannel.Sms
        val request =
            ForgotPasswordResetRequest(
                newPassword = newPassword,
                phone = identifier.takeIf { sms },
                email = identifier.takeUnless { sms },
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
                // The iOS Keychain survives a reinstall; drop its token on the first launch. The analytics identity
                // goes only with a wiped token: a reset on a plain first launch would give the install a second
                // anonymous id, and `Application Installed` would never join the signup that follows it.
                var wiped = false
                if (!settings.getBoolean(KEY_HAS_LAUNCHED, false)) {
                    wiped = tokenStorage.token != null
                    tokenStorage.clear()
                    copilot.clear()
                    if (wiped) resetAnalytics()
                    settings[KEY_HAS_LAUNCHED] = true
                }
                if (tokenStorage.token == null) {
                    analytics.track(AnalyticsEvent.SessionRestored(if (wiped) "reinstall_wipe" else "no_token"))
                    return@withContext false
                }
                // A slow answer counts as offline: the token stays and Home opens (only a 401 means Login).
                val reply =
                    withTimeoutOrNull(
                        RESTORE_TIMEOUT_MS,
                    ) { apiCall<Unit>("") { client.get(ApiConfig.Endpoints.PROFILE) } }
                val rejected = if (reply == null) ApiException.NetworkError("") else reply.exceptionOrNull()
                if (rejected is ApiException.Unauthorized) {
                    tokenStorage.clear()
                    copilot.clear()
                    resetAnalytics()
                }
                analytics.track(
                    AnalyticsEvent.SessionRestored(
                        when (rejected) {
                            null -> "signed_in"
                            is ApiException.Unauthorized -> "expired"
                            is ApiException.NetworkError -> "offline"
                            else -> "server"
                        },
                    ),
                )
                rejected !is ApiException.Unauthorized
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
        }

    /** 05 §2.8: unregisters push (P7 with "", errors ignored), then drops the token and the Shifa AI chats. No logout endpoint.
     * [source]: `profile` / `account_deleted` / `backend_switch` (events.md §3).
     */
    suspend fun logout(source: String = "profile") {
        analytics.track(AnalyticsEvent.LoggedOut(source))
        apiCall<String>("") {
            client.put(ApiConfig.Endpoints.FCM_TOKEN) {
                contentType(ContentType.Application.Json)
                setBody(FcmTokenRequest(""))
                // Best effort: Log out must not hang for the client's 30 s on a dead network.
                timeout { requestTimeoutMillis = LOGOUT_TIMEOUT_MS }
            }
        }
        tokenStorage.clear()
        copilot.clear()
        Push.opened.value = null // a push tapped for this account is not routed for the next one
        // The analytics identity goes with the session (docs/posthog/events.md §1.2).
        resetAnalytics()
    }
}

private const val LOGOUT_TIMEOUT_MS = 5_000L
private const val RESTORE_TIMEOUT_MS = 5_000L // the splash does not wait longer than this for the profile call

/** A5 failures that switch registration to the email code (05 §2.4 step 5): SMS unavailable or rate limited. */
fun Throwable.needsEmailFallback() =
    this is ApiException.RateLimit ||
        message in setOf(SMS_COULD_NOT_BE_SENT, TOO_MANY_VERIFICATION_REQUESTS, TOO_MANY_REQUESTS)
