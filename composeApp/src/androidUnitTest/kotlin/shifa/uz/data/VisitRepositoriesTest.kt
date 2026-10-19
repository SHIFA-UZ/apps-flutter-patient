package shifa.uz.data

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
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorVideoCallPaymentRequired
import shifa.uz.data.api.apiJson
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.storage.TokenStorage
import shifa.uz.data.video.VideoRepository

/** The live visit's calls: B4, B6, V1. */
class VisitRepositoriesTest {
    private var body = "{}"
    private var status = HttpStatusCode.OK
    private val engine = MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
    private val client = createHttpClient(TokenStorage(MapSettings()), engine)
    private val request get() = engine.requestHistory.last()

    @Test
    fun `B4 reads the signing flags and the location`() =
        runTest {
            body =
                """{"id":256,"startAt":"2026-04-28T09:30:00Z","location":"SEBAMED","locationLabel":"SEBAMED",""" +
                """"locationAddress":"Tinchlik metro","signatureRequested":true,"alreadySigned":false}"""

            val visit = BookingsRepository(client).getAppointment("256").getOrThrow()

            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/appointments/256")
            assertThat(visit.needsSignature).isTrue()
            assertThat(visit.locationAddress).isEqualTo("Tinchlik metro")
            assertThat(visit.copy(alreadySigned = true).needsSignature).isFalse()
        }

    @Test
    fun `B6 posts the PNG as plain base64`() =
        runTest {
            BookingsRepository(client).submitSignature("256", byteArrayOf(1, 2, 3)).getOrThrow()

            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/appointments/256/submit-signature")
            assertThat(apiJson.parseToJsonElement(request.body.toByteArray().decodeToString()))
                .isEqualTo(apiJson.parseToJsonElement("""{"signatureImageBase64":"AQID"}"""))
        }

    @Test
    fun `V1 sends a numeric id, needs a token and a room, and says when payment is due`() =
        runTest {
            body = """{"token":"t","roomUrl":"https://shifa.daily.co/room","roomName":"room"}"""
            val videos = VideoRepository(client)

            assertThat(videos.token("42").getOrThrow().roomUrl).isEqualTo("https://shifa.daily.co/room")
            assertThat(request.url.encodedPath).isEqualTo("/api/video/token")
            assertThat(apiJson.parseToJsonElement(request.body.toByteArray().decodeToString()))
                .isEqualTo(apiJson.parseToJsonElement("""{"appointmentId":42}"""))

            body = """{"token":""}"""
            assertThat(videos.token("42").isFailure).isTrue()

            body = """{"message":"Payment is required before joining this video consultation."}"""
            status = HttpStatusCode.Forbidden
            assertThat(
                videos.token("42").exceptionOrNull()!!.toUserMessage(),
            ).isEqualTo(Res.string.errorVideoCallPaymentRequired)
        }
}
