// JSON fixtures stay on one line, like the backend sends them.
@file:Suppress("ktlint:standard:max-line-length")

package shifa.uz.data.chat

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import shifa.uz.data.api.apiJson
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.model.MessageType
import shifa.uz.data.model.SendMessageRequest
import shifa.uz.data.storage.TokenStorage

class ChatRepositoryTest {
    private var body = "[]"
    private val engine =
        MockEngine { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
    private val tokens = TokenStorage(MapSettings()).apply { token = "t0k" }
    private val classToTest = ChatRepository(createHttpClient(tokens, engine))

    private val request get() = engine.requestHistory.single()

    @Test
    fun `the list reads numeric ids and skips items without one`() =
        runTest {
            body = """[{"id":3,"participantId":11,"participantName":"Aziz","lastMessageAt":"2026-09-28T08:40:00Z","unreadCount":2},{"participantName":"X"}]"""

            val list = classToTest.conversations().getOrThrow()

            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/messages/conversations")
            assertThat(list.map { it.id to it.participantId }).containsExactly("3" to "11")
            assertThat(list[0].lastMessageAt.toString()).isEqualTo("2026-09-28T08:40:00Z")
            assertThat(list[0].unreadCount).isEqualTo(2)
        }

    @Test
    fun `a chat reads both message formats`() =
        runTest {
            body =
                """{"conversation":{"id":3,"participantName":"Aziz"},"messages":[
                    {"id":1,"text":"hi","createdAt":"2026-09-28T08:00:00Z","isMine":true},
                    {"id":2,"attachmentUrl":"https://f/a.jpg","createdAt":"2026-09-28T08:01:00Z"},
                    {"id":3,"attachmentUrl":"https://f/x","attachmentName":"Results.PDF","createdAt":"2026-09-28T08:02:00Z"},
                    {"id":4,"type":"voice","content":{"url":"https://f/v.m4a","duration":42.6},"createdAt":"2026-09-28T08:03:00Z"},
                    {"id":5,"type":"sticker","content":{"text":"?"},"createdAt":"2026-09-28T08:04:00Z"},
                    {"id":6,"text":"no date"}
                ]}"""

            val chat = classToTest.conversation("3").getOrThrow()

            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/messages/conversations/3")
            assertThat(chat.contact?.participantName).isEqualTo("Aziz")
            assertThat(chat.messages.map { it.type })
                .containsExactly(
                    MessageType.Text,
                    MessageType.Image,
                    MessageType.Document,
                    MessageType.Voice,
                    MessageType.Text,
                )
                .inOrder()
            assertThat(chat.messages[0].body).isEqualTo("hi")
            assertThat(chat.messages[0].isMine).isTrue()
            assertThat(chat.messages[2].fileName).isEqualTo("Results.PDF")
            assertThat(chat.messages[3].fileUrl).isEqualTo("https://f/v.m4a")
            assertThat(chat.messages[3].durationSeconds).isEqualTo(42)
        }

    @Test
    fun `a send leaves nulls out and an echo that doesn't parse is null`() =
        runTest {
            body = """{"ok":true}"""

            val sent = classToTest.send(SendMessageRequest(recipientDoctorId = 15, text = "Hello")).getOrThrow()

            assertThat(sent).isNull()
            assertThat(apiJson.parseToJsonElement(request.body.toByteArray().decodeToString()))
                .isEqualTo(apiJson.parseToJsonElement("""{"recipientDoctorId":15,"text":"Hello"}"""))
        }
}
