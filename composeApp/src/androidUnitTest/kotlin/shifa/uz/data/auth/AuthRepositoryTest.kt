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
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import shifa.uz.data.api.LOGIN_FAILED
import shifa.uz.data.api.NETWORK_ERROR
import shifa.uz.data.api.NO_TOKEN_RECEIVED
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.storage.TokenStorage
import java.io.IOException

class AuthRepositoryTest {
    private val settings = MapSettings("has_launched_before" to true)
    private val tokens = TokenStorage(MapSettings())
    private var reply: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respondOk() }
    private val engine = MockEngine { reply(it) }
    private val client = createHttpClient(tokens, engine)
    private val classToTest = AuthRepository(client, tokens, settings)

    private fun replyJson(
        status: HttpStatusCode,
        body: String,
    ) {
        reply = { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
    }

    private suspend fun loginError(): ApiException = classToTest.login("u", "p").exceptionOrNull() as ApiException

    private val request get() = engine.requestHistory.single()

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
    fun `the first launch wipes a token left by a reinstall`() =
        runTest {
            val fresh = MapSettings()
            tokens.token = "t"

            assertThat(AuthRepository(client, tokens, fresh).restoreSession()).isFalse()
            assertThat(tokens.token).isNull()
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
    fun `a 401 on restore signs out and clears the token`() =
        runTest {
            tokens.token = "t"
            replyJson(HttpStatusCode.Unauthorized, "{}")

            assertThat(classToTest.restoreSession()).isFalse()
            assertThat(tokens.token).isNull()
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
