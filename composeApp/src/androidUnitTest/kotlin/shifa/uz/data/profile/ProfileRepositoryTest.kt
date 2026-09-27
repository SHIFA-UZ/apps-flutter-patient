package shifa.uz.data.profile

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.storage.TokenStorage

class ProfileRepositoryTest {
    private var body = """{"photoUrl":"https://cdn/p.jpg"}"""
    private val engine = MockEngine { respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
    private val classToTest = ProfileRepository(createHttpClient(TokenStorage(MapSettings()), engine))

    private val request get() = engine.requestHistory.single()

    @Test
    fun `the photo URL is read from an object or a bare string`() {
        assertThat(photoUrl("""{"photoUrl":"https://a"}""")).isEqualTo("https://a")
        assertThat(photoUrl("\"https://b\"")).isEqualTo("https://b")
        assertThat(photoUrl(" https://c ")).isEqualTo("https://c")
        assertThat(photoUrl("""{"url":"https://d"}""")).isNull()
        assertThat(photoUrl("""{"photoUrl":""}""")).isNull()
        assertThat(photoUrl("")).isNull()
    }

    @Test
    fun `the photo goes as the multipart field file`() =
        runTest {
            assertThat(
                classToTest.uploadPhoto(byteArrayOf(1, 2, 3), "me.jpg").getOrThrow(),
            ).isEqualTo("https://cdn/p.jpg")

            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/photo")
            val multipart = request.body.toByteArray().decodeToString()
            assertThat(multipart).contains("name=file")
            assertThat(multipart).contains("filename=\"me.jpg\"")
            assertThat(multipart).contains("Content-Type: image/jpeg")
        }

    @Test
    fun `a body without a URL fails the upload`() =
        runTest {
            body = "{}"

            assertThat(classToTest.uploadPhoto(byteArrayOf(1), "me.jpg").isFailure).isTrue()
        }

    @Test
    fun `the profile patch sends only the set keys`() =
        runTest {
            body = "{}"

            classToTest.updateProfile(ProfileUpdate(birthDate = "1990-05-01", locationCountry = "Germany")).getOrThrow()

            assertThat(request.method).isEqualTo(HttpMethod.Patch)
            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/profile")
            assertThat(Json.parseToJsonElement((request.body as TextContent).text))
                .isEqualTo(Json.parseToJsonElement("""{"birthDate":"1990-05-01","locationCountry":"Germany"}"""))
        }
}
