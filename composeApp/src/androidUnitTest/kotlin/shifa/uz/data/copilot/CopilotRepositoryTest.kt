package shifa.uz.data.copilot

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import shifa.uz.data.api.COPILOT_FAILED
import shifa.uz.data.api.NETWORK_ERROR
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CopilotMessage
import shifa.uz.data.storage.TokenStorage
import java.io.IOException

class CopilotRepositoryTest {
    private var status = HttpStatusCode.OK
    private var body = ""
    private var down = false
    private val engine =
        MockEngine {
            if (down) throw IOException("down")
            respond(body, status, headersOf(HttpHeaders.ContentType, "text/event-stream"))
        }
    private val tokens = TokenStorage(MapSettings()).apply { token = "t0k" }
    private val classToTest = CopilotRepository(createHttpClient(tokens, engine))
    private val streamed = mutableListOf<String>()

    private suspend fun stream() =
        classToTest.stream(listOf(CopilotMessage(CopilotMessage.USER, "Hi")), "RU") {
            streamed += it
        }

    @Test
    fun `X1 posts the turns and streams each event, spaces and lines kept, data left at EOF included`() =
        runTest {
            body = "data: Hello\n\ndata:,\ndata: world\n\nevent: ping\n\n: comment\ndata: !"

            val reply = stream().getOrThrow()

            assertThat(streamed).containsExactly(" Hello", ",\n world", " !").inOrder()
            assertThat(reply).isEqualTo("Hello,\n world !")
            val request = engine.requestHistory.single()
            assertThat(request.method).isEqualTo(HttpMethod.Post)
            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/copilot/stream")
            assertThat(request.headers[HttpHeaders.Accept]).isEqualTo("text/event-stream")
            assertThat(request.headers[HttpHeaders.Authorization]).isEqualTo("Bearer t0k")
            // The app's Json pretty-prints.
            assertThat(Json.parseToJsonElement(request.body.toByteArray().decodeToString()))
                .isEqualTo(Json.parseToJsonElement("""{"messages":[{"role":"user","content":"Hi"}],"language":"RU"}"""))
        }

    @Test
    fun `a 429 or a RATE_LIMIT code is the rate limit, other statuses the generic error`() =
        runTest {
            status = HttpStatusCode.TooManyRequests
            assertThat(stream().exceptionOrNull()).isInstanceOf(ApiException.RateLimit::class.java)

            status = HttpStatusCode.Forbidden
            body = """{"code":"RATE_LIMIT","message":"Daily limit"}"""
            assertThat(stream().exceptionOrNull()).isInstanceOf(ApiException.RateLimit::class.java)

            status = HttpStatusCode.InternalServerError
            body = """{"message":"NullPointerException at x.y"}"""
            val error = stream().exceptionOrNull()
            assertThat(error).isInstanceOf(ApiException.ServerError::class.java)
            assertThat(error).hasMessageThat().isEqualTo(COPILOT_FAILED)
        }

    @Test
    fun `an error event stops the stream, and only RATE_LIMIT keeps its meaning`() =
        runTest {
            body = "data: Hi\n\nevent: error\ndata: {\"code\":\"RATE_LIMIT\"}\n\ndata: never\n\n"
            assertThat(stream().exceptionOrNull()).isInstanceOf(ApiException.RateLimit::class.java)
            assertThat(streamed).containsExactly(" Hi")

            body = "event: error\ndata: {\"code\":\"SAFETY\",\"message\":\"Blocked by policy\"}\n\n"
            val error = stream().exceptionOrNull()
            assertThat(error).isInstanceOf(ApiException.ServerError::class.java)
            assertThat(error).hasMessageThat().isEqualTo(COPILOT_FAILED)
        }

    @Test
    fun `no connection is a network error`() =
        runTest {
            down = true

            val error = stream().exceptionOrNull()

            assertThat(error).isInstanceOf(ApiException.NetworkError::class.java)
            assertThat(error).hasMessageThat().isEqualTo(NETWORK_ERROR)
        }
}
