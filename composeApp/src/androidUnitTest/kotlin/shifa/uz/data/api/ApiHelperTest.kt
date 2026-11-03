package shifa.uz.data.api

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.RecordingTracker
import shifa.uz.data.storage.TokenStorage

class ApiHelperTest {
    private val tracker = RecordingTracker()
    private val previous = apiAnalytics

    @Before
    fun setUp() {
        apiAnalytics = tracker
    }

    @After
    fun tearDown() {
        apiAnalytics = previous
    }

    @Test
    fun `a failed call is tracked by its endpoint template, status and kind, never its message`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        """{"message":"Appointment 123 of Aziz not found","code":"NOT_FOUND"}""",
                        HttpStatusCode.NotFound,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = createHttpClient(TokenStorage(MapSettings()), engine)

            apiCall<Unit>("x") { client.get("patients/me/appointments/123") }

            assertThat(tracker.events)
                .containsExactly(
                    AnalyticsEvent.ApiError("patients/me/appointments/{id}", "GET", 404, "NotFound", "NOT_FOUND"),
                )
            assertThat(tracker.events.single().properties.values.map { it.toString() }).doesNotContain("Aziz")
        }

    @Test
    fun `only the API's own words survive in an endpoint`() {
        assertThat(endpointTemplate("/api/patients/me/appointments/123/review"))
            .isEqualTo("patients/me/appointments/{id}/review")
        assertThat(endpointTemplate("/api/public/doctors/3fa85f64-5717-4562-b3fc-2c963f66afa6"))
            .isEqualTo("public/doctors/{id}")
        assertThat(endpointTemplate("/api/notifications/page")).isEqualTo("notifications/page")
        assertThat(endpointTemplate("/api/files/Aziz.pdf")).isEqualTo("{id}/{id}")
    }

    @Test
    fun `a file host is external, with no path and no code`() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        """{"message":"gone","code":"Aziz Karimov"}""",
                        HttpStatusCode.Forbidden,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = createHttpClient(TokenStorage(MapSettings()), engine)

            apiCall<Unit>("x") { client.get("https://files.example.com/patients/Aziz.pdf") }

            assertThat(
                tracker.events,
            ).containsExactly(AnalyticsEvent.ApiError("external", "GET", 403, "Forbidden", null))
        }
}
