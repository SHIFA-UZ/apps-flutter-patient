package shifa.uz.data.auth

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import shifa.uz.data.api.LOGIN_FAILED
import shifa.uz.data.api.NETWORK_ERROR
import shifa.uz.data.api.NO_TOKEN_RECEIVED
import shifa.uz.data.api.SMS_COULD_NOT_BE_SENT
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CopilotChat
import shifa.uz.data.model.OtpChannel
import shifa.uz.data.model.RegisterPatientRequest
import shifa.uz.data.storage.CopilotStorage
import shifa.uz.data.storage.TokenStorage
import java.io.IOException

class AuthRepositoryTest {
    private val settings = MapSettings("has_launched_before" to true)
    private val tokens = TokenStorage(MapSettings())
    private var reply: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respondOk() }
    private val engine = MockEngine { reply(it) }
    private val client = createHttpClient(tokens, engine)
    private val chats = CopilotStorage(MapSettings()).apply { save(listOf(CopilotChat("1"))) }
    private val classToTest = AuthRepository(client, tokens, settings, chats)

    private fun replyJson(
        status: HttpStatusCode,
        body: String,
    ) {
        reply = { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
    }

    private suspend fun loginError(): ApiException = classToTest.login("u", "p").exceptionOrNull() as ApiException

    private val request get() = engine.requestHistory.single()

    private val requestJson get() = Json.parseToJsonElement((request.body as TextContent).text)

    @Test
    fun `login posts the credentials as a patient without a token and saves the new one`() =
        runTest {
            tokens.token = "old"
            replyJson(HttpStatusCode.OK, """{"token":"t1","forcePasswordReset":true}""")

            val response = classToTest.login("+998901234567", " p ").getOrThrow()

            assertThat(response.forcePasswordReset).isTrue()
            assertThat(tokens.token).isEqualTo("t1")
            assertThat(request.url.toString())
                .isEqualTo("https://shifa-doc-backend-mvp-production.up.railway.app/api/auth/login?app=patient")
            assertThat(request.headers[HttpHeaders.Authorization]).isNull()
            assertThat(Json.parseToJsonElement((request.body as TextContent).text))
                .isEqualTo(Json.parseToJsonElement("""{"username":"+998901234567","password":" p "}"""))
        }

    @Test
    fun `an unknown 401 message becomes invalid credentials`() =
        runTest {
            replyJson(HttpStatusCode.Unauthorized, """{"message":"Bad credentials for user 42"}""")

            val error = loginError()
            assertThat(error).isInstanceOf(ApiException.Unauthorized::class.java)
            assertThat(error.message).isEqualTo("Invalid credentials")
        }

    @Test
    fun `a known message passes through without its login prefix`() =
        runTest {
            replyJson(HttpStatusCode.Forbidden, """{"message":"Login error: Account is disabled"}""")

            val error = loginError()
            assertThat(error).isInstanceOf(ApiException.Forbidden::class.java)
            assertThat(error.message).isEqualTo("Account is disabled")
        }

    @Test
    fun `the error field is read when there is no message`() =
        runTest {
            replyJson(HttpStatusCode.BadRequest, """{"error":"Username is required"}""")

            assertThat(loginError().message).isEqualTo("Username is required")
        }

    @Test
    fun `a technical server error falls back to login failed`() =
        runTest {
            replyJson(
                HttpStatusCode.InternalServerError,
                """{"error":"NullPointerException at AuthController.kt:42"}""",
            )

            val error = loginError()
            assertThat(error).isInstanceOf(ApiException.ServerError::class.java)
            assertThat(error.message).isEqualTo(LOGIN_FAILED)
        }

    @Test
    fun `a response without a token fails and saves nothing`() =
        runTest {
            replyJson(HttpStatusCode.OK, "{}")

            assertThat(loginError().message).isEqualTo(NO_TOKEN_RECEIVED)
            assertThat(tokens.token).isNull()
        }

    @Test
    fun `no response is a network error`() =
        runTest {
            reply = { throw IOException("offline") }

            val error = loginError()
            assertThat(error).isInstanceOf(ApiException.NetworkError::class.java)
            assertThat(error.message).isEqualTo(NETWORK_ERROR)
        }

    @Test
    fun `check-identifier sends only the given keys and reads the type`() =
        runTest {
            replyJson(HttpStatusCode.OK, """{"type":"doctor","firstName":"Aziz","lastName":"Karimov"}""")

            val response = classToTest.checkIdentifier("+998901234567", null).getOrThrow()

            assertThat(response.type).isEqualTo("doctor")
            assertThat(response.firstName).isEqualTo("Aziz")
            assertThat(request.url.encodedPath).isEqualTo("/api/auth/check-identifier")
            assertThat(requestJson).isEqualTo(Json.parseToJsonElement("""{"phone":"+998901234567"}"""))
        }

    @Test
    fun `no account is none whether it is a 404, null or a missing type`() =
        runTest {
            for ((status, body) in listOf(
                HttpStatusCode.NotFound to "{}",
                HttpStatusCode.OK to "null",
                HttpStatusCode.OK to "{}",
            )) {
                replyJson(status, body)
                assertThat(classToTest.checkIdentifier(null, "a@shifa.uz").getOrThrow().type).isEqualTo("none")
            }
        }

    @Test
    fun `send-sms-otp reports where the code went`() =
        runTest {
            replyJson(HttpStatusCode.OK, """{"message":"sent"}""")
            assertThat(classToTest.sendSmsOtp("+998901234567").getOrThrow()).isEqualTo(OtpChannel.Sms)

            replyJson(HttpStatusCode.OK, """{"channel":"EMAIL"}""")
            assertThat(classToTest.sendSmsOtp("+998901234567").getOrThrow()).isEqualTo(OtpChannel.Email)
        }

    @Test
    fun `SMS failures that switch to email`() =
        runTest {
            replyJson(HttpStatusCode.ServiceUnavailable, """{"message":"$SMS_COULD_NOT_BE_SENT"}""")
            assertThat(classToTest.sendSmsOtp("+998901234567").exceptionOrNull()!!.needsEmailFallback()).isTrue()

            replyJson(HttpStatusCode.TooManyRequests, "{}")
            assertThat(classToTest.sendSmsOtp("+998901234567").exceptionOrNull()!!.needsEmailFallback()).isTrue()

            replyJson(HttpStatusCode.InternalServerError, "{}")
            assertThat(classToTest.sendSmsOtp("+998901234567").exceptionOrNull()!!.needsEmailFallback()).isFalse()
        }

    @Test
    fun `register omits null keys and saves the token`() =
        runTest {
            replyJson(HttpStatusCode.OK, """{"token":"t2"}""")

            classToTest.registerPatient(
                RegisterPatientRequest("Ali", "Valiyev", "Secret1!", "+998901234567", smsOtp = "123456"),
            )
                .getOrThrow()

            assertThat(tokens.token).isEqualTo("t2")
            assertThat(requestJson).isEqualTo(
                Json.parseToJsonElement(
                    """{"firstName":"Ali","lastName":"Valiyev","password":"Secret1!",""" +
                        """"phone":"+998901234567","smsOtp":"123456"}""",
                ),
            )
        }

    @Test
    fun `send-forgot-password-otp sends the identifier as a patient and defaults to email`() =
        runTest {
            replyJson(HttpStatusCode.OK, "{}")
            assertThat(classToTest.sendForgotPasswordOtp("a@shifa.uz").getOrThrow()).isEqualTo(OtpChannel.Email)
            assertThat(request.url.encodedPath).isEqualTo("/api/auth/send-forgot-password-otp")
            assertThat(request.headers[HttpHeaders.Authorization]).isNull()
            assertThat(
                requestJson,
            ).isEqualTo(Json.parseToJsonElement("""{"identifier":"a@shifa.uz","app":"patient"}"""))

            replyJson(HttpStatusCode.OK, """{"channel":"sms"}""")
            assertThat(classToTest.sendForgotPasswordOtp("+998901234567").getOrThrow()).isEqualTo(OtpChannel.Sms)
        }

    @Test
    fun `an email reset sends email and emailOtp, then saves the token`() =
        runTest {
            replyJson(HttpStatusCode.OK, """{"token":"t3"}""")

            classToTest.resetForgottenPassword("a@shifa.uz", OtpChannel.Email, "123456", "Secret1!").getOrThrow()
            assertThat(tokens.token).isEqualTo("t3")
            assertThat(request.url.encodedPath).isEqualTo("/api/auth/forgot-password-reset")
            assertThat(requestJson).isEqualTo(
                Json.parseToJsonElement(
                    """{"newPassword":"Secret1!","email":"a@shifa.uz","emailOtp":"123456","app":"patient"}""",
                ),
            )
        }

    @Test
    fun `a phone reset sends phone, and a code the server emailed goes as emailOtp (05 Q13)`() =
        runTest {
            replyJson(HttpStatusCode.OK, """{"token":"t4"}""")
            classToTest.resetForgottenPassword("+998901234567", OtpChannel.Sms, "111111", "Secret1!").getOrThrow()
            classToTest.resetForgottenPassword("+998901234567", OtpChannel.Email, "222222", "Secret1!").getOrThrow()

            val (sms, email) =
                engine.requestHistory.map { Json.parseToJsonElement((it.body as TextContent).text) }
            assertThat(sms).isEqualTo(
                Json.parseToJsonElement(
                    """{"newPassword":"Secret1!","phone":"+998901234567","smsOtp":"111111","app":"patient"}""",
                ),
            )
            assertThat(email).isEqualTo(
                Json.parseToJsonElement(
                    """{"newPassword":"Secret1!","phone":"+998901234567","emailOtp":"222222","app":"patient"}""",
                ),
            )
        }

    @Test
    fun `a failed reset saves nothing and falls back to failed to reset password`() =
        runTest {
            replyJson(HttpStatusCode.BadRequest, """{"message":"stack trace"}""")

            val error = classToTest.resetForgottenPassword("a@shifa.uz", OtpChannel.Email, "1", "p").exceptionOrNull()

            assertThat(error!!.message).isEqualTo("Failed to reset password")
            assertThat(tokens.token).isNull()
        }

    @Test
    fun `the deletion code sends its purpose`() =
        runTest {
            classToTest.sendEmailOtp("a@shifa.uz", "ACCOUNT_DELETION").getOrThrow()

            assertThat(
                requestJson,
            ).isEqualTo(Json.parseToJsonElement("""{"email":"a@shifa.uz","purpose":"ACCOUNT_DELETION"}"""))
        }

    @Test
    fun `change password posts both passwords with the token, and a wrong current one keeps its message`() =
        runTest {
            tokens.token = "t"
            classToTest.changePassword("old", "New1!pass").getOrThrow()

            assertThat(request.url.encodedPath).isEqualTo("/api/auth/reset-password")
            assertThat(request.headers[HttpHeaders.Authorization]).isEqualTo("Bearer t")
            assertThat(requestJson)
                .isEqualTo(Json.parseToJsonElement("""{"currentPassword":"old","newPassword":"New1!pass"}"""))

            // The backend's ErrorHandler body: `error` is the status number.
            replyJson(
                HttpStatusCode.BadRequest,
                """{"error":400,"message":"Current password is incorrect","status":400}""",
            )
            assertThat(
                classToTest.changePassword("x", "y").exceptionOrNull()!!.message,
            ).isEqualTo("Current password is incorrect")
            assertThat(tokens.token).isEqualTo("t")
        }

    @Test
    fun `logout clears the push token, then the session and the Shifa AI chats, even when that call fails`() =
        runTest {
            tokens.token = "t"
            replyJson(HttpStatusCode.InternalServerError, "{}")

            classToTest.logout()

            assertThat(request.method).isEqualTo(HttpMethod.Put)
            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/fcm-token")
            assertThat(request.headers[HttpHeaders.Authorization]).isEqualTo("Bearer t")
            assertThat(requestJson).isEqualTo(Json.parseToJsonElement("""{"fcmToken":""}"""))
            assertThat(tokens.token).isNull()
            assertThat(chats.load()).isEmpty()
        }

    @Test
    fun `the first launch wipes a token left by a reinstall`() =
        runTest {
            val fresh = MapSettings()
            tokens.token = "t"

            assertThat(AuthRepository(client, tokens, fresh, chats).restoreSession()).isFalse()
            assertThat(tokens.token).isNull()
            assertThat(chats.load()).isEmpty()
            assertThat(fresh.getBoolean("has_launched_before", false)).isTrue()
            assertThat(engine.requestHistory).isEmpty()
        }

    @Test
    fun `no token means signed out without a request`() =
        runTest {
            assertThat(classToTest.restoreSession()).isFalse()
            assertThat(engine.requestHistory).isEmpty()
        }

    @Test
    fun `a stored token is checked against the profile`() =
        runTest {
            tokens.token = "t"

            assertThat(classToTest.restoreSession()).isTrue()
            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/profile")
            assertThat(request.headers[HttpHeaders.Authorization]).isEqualTo("Bearer t")
        }

    @Test
    fun `a 401 on restore signs out and clears the token and the chats`() =
        runTest {
            tokens.token = "t"
            replyJson(HttpStatusCode.Unauthorized, "{}")

            assertThat(classToTest.restoreSession()).isFalse()
            assertThat(tokens.token).isNull()
            assertThat(chats.load()).isEmpty()
        }

    @Test
    fun `server errors and no network keep the session`() =
        runTest {
            tokens.token = "t"
            replyJson(HttpStatusCode.InternalServerError, "{}")
            assertThat(classToTest.restoreSession()).isTrue()

            reply = { throw IOException("offline") }
            assertThat(classToTest.restoreSession()).isTrue()
            assertThat(tokens.token).isEqualTo("t")
        }

    @Test
    fun `the token is never sent to another host`() =
        runTest {
            tokens.token = "t"

            client.get("https://files.example.com/api/patients/me/profile")

            assertThat(request.headers[HttpHeaders.Authorization]).isNull()
        }
}
