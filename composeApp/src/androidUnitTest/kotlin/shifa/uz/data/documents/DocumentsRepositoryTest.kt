package shifa.uz.data.documents

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
import org.junit.Test
import shifa.uz.data.api.createHttpClient
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.documentExtension
import shifa.uz.data.storage.TokenStorage

class DocumentsRepositoryTest {
    private var body = "[]"
    private val engine =
        MockEngine { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
    private val tokens = TokenStorage(MapSettings()).apply { token = "t0k" }
    private val classToTest = DocumentsRepository(createHttpClient(tokens, engine))

    private val request get() = engine.requestHistory.single()

    @Test
    fun `the list reads numeric ids and skips broken items`() =
        runTest {
            body = """[{"id":7,"title":"Blood","createdAt":"2026-09-01T10:00:00Z","url":"https://f/a.pdf","creatorLabel":"Patient"},{"id":{}},{"title":"X"}]"""

            val list = classToTest.getDocuments().getOrThrow()

            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/documents")
            assertThat(list.map { it.id }).containsExactly("7", "").inOrder()
            assertThat(list[0].file).isEqualTo("https://f/a.pdf")
            assertThat(list[0].displayDate).isEqualTo("2026-09-01T10:00:00Z")
            assertThat(list[0].isUploadedBy(null)).isTrue()
            // The backend sends the uploader's name, not the role.
            assertThat(list[0].copy(creatorLabel = "Botir Muzrabov").isUploadedBy(" Botir Muzrabov")).isTrue()
            assertThat(list[0].copy(creatorLabel = "Nigora Omarova").isUploadedBy("Botir Muzrabov")).isFalse()
            assertThat(list[0].copy(creatorLabel = null).isUploadedBy(null)).isFalse()
        }

    @Test
    fun `the upload sends the file, title and category as multipart`() =
        runTest {
            body = """{"id":"9","title":"Scan"}"""

            val doc = classToTest.upload(byteArrayOf(1), "a\"b/c.pdf", "Scan", "MRI").getOrThrow()

            assertThat(doc).isEqualTo(PatientDocument(id = "9", title = "Scan"))
            assertThat(request.method).isEqualTo(HttpMethod.Post)
            val multipart = request.body.toByteArray().decodeToString()
            assertThat(multipart).contains("filename=\"a_b_c.pdf\"")
            assertThat(multipart).contains("Content-Type: application/pdf")
            assertThat(multipart).contains("name=isChatAttachment")
            assertThat(multipart).contains("name=category")
        }

    @Test
    fun `no category leaves the field out`() =
        runTest {
            body = "{}"

            classToTest.upload(byteArrayOf(1), "a.jpg", "A", null).getOrThrow()

            assertThat(request.body.toByteArray().decodeToString()).doesNotContain("name=category")
        }

    @Test
    fun `delete hits the document path`() =
        runTest {
            body = ""

            classToTest.delete("12").getOrThrow()

            assertThat(request.method).isEqualTo(HttpMethod.Delete)
            assertThat(request.url.encodedPath).isEqualTo("/api/patients/me/documents/12")
        }

    @Test
    fun `a file on another host gets no token`() =
        runTest {
            body = "x"

            assertThat(
                classToTest.getFile("https://files.example.com/a.pdf").getOrThrow(),
            ).isEqualTo("x".encodeToByteArray())
            assertThat(request.headers[HttpHeaders.Authorization]).isNull()
        }

    @Test
    fun `an empty file fails`() =
        runTest {
            body = ""

            assertThat(classToTest.getFile("https://files.example.com/a.pdf").isFailure).isTrue()
        }

    @Test
    fun `the bytes pick the viewer before the type or URL`() {
        val pdf = "%PDF-1.4".encodeToByteArray()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val webp = "RIFF\u0000\u0000\u0000\u0000WEBP".encodeToByteArray()
        val jpg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val other = byteArrayOf(1, 2, 3)

        assertThat(documentExtension(pdf, "image/png", "https://f/a.png")).isEqualTo("pdf")
        assertThat(documentExtension(png, "pdf", null)).isEqualTo("png")
        assertThat(documentExtension(webp, null, null)).isEqualTo("webp")
        assertThat(documentExtension(jpg, null, null)).isEqualTo("jpg")
        assertThat(documentExtension(other, "application/pdf", null)).isEqualTo("pdf")
        assertThat(documentExtension(other, "image/gif", null)).isEqualTo("gif")
        assertThat(documentExtension(other, null, "https://f/b.JPEG?sig=1")).isEqualTo("jpeg")
        assertThat(documentExtension(other, "docx", "https://f/b.docx")).isNull()
    }
}
