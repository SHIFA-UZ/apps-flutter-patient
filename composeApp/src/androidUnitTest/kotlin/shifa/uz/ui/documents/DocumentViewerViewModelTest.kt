package shifa.uz.ui.documents

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.documentSaved
import shifa.composeapp.generated.resources.errorOpeningDocument
import shifa.composeapp.generated.resources.errorSomethingWentWrong
import shifa.uz.data.api.FAILED_TO_DELETE_DOCUMENT
import shifa.uz.data.bookings.BookingsRepository
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.PatientDocument

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentViewerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val documents = mock<DocumentsRepository>()
    private val bookings = mock<BookingsRepository>()
    private val visit = Appointment(id = "42", doctorName = "Aziz Karimov", startAt = "2026-09-20T05:00:00Z")
    private val otherDay = Appointment(id = "43", doctorName = "Aziz Karimov", startAt = "2026-09-21T05:00:00Z")
    private val pdf = "%PDF-1.4".encodeToByteArray()
    private val document =
        PatientDocument(id = "5", title = "Lab: result", fileUrl = "https://f/5", creatorLabel = "Patient")
    private lateinit var classToTest: DocumentViewerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { documents.getFile("https://f/5") } doReturn Result.success(pdf)
        wheneverBlocking { bookings.getAppointments() } doReturn Result.success(listOf(visit, otherDay))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create(doc: PatientDocument = document) {
        classToTest = DocumentViewerViewModel(doc, documents, bookings)
    }

    private val state get() = classToTest.state.value

    @Test
    fun `it loads the file and tells its type from the bytes`() =
        runTest {
            create()
            assertThat(state.isLoading).isTrue()
            advanceUntilIdle()

            assertThat(state.bytes).isEqualTo(pdf)
            assertThat(state.extension).isEqualTo("pdf")
            assertThat(state.canDownload).isTrue()
        }

    @Test
    fun `no URL shows an error without a request`() =
        runTest {
            create(document.copy(fileUrl = null))
            advanceUntilIdle()

            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isEqualTo(Res.string.errorOpeningDocument)
            verifyBlocking(documents, never()) { getFile(any()) }
        }

    @Test
    fun `a 401 from the file host is only an error, and Retry loads again`() =
        runTest {
            wheneverBlocking {
                documents.getFile(
                    "https://f/5",
                )
            } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))
            create()
            advanceUntilIdle()
            assertThat(state.bytes).isNull()
            assertThat(state.error).isNotNull()

            wheneverBlocking { documents.getFile("https://f/5") } doReturn Result.success(pdf)
            classToTest.load()
            advanceUntilIdle()

            assertThat(state.bytes).isEqualTo(pdf)
            assertThat(state.error).isNull()
        }

    @Test
    fun `download saves under a safe title with the extension`() =
        runTest {
            create()
            advanceUntilIdle()
            var saved: String? = null

            classToTest.download { name, _ -> Result.success(name).also { saved = name } }

            assertThat(saved).isEqualTo("Lab_ result.pdf")
            assertThat(state.notice).isEqualTo(Res.string.documentSaved)
        }

    @Test
    fun `a failed save shows an error`() =
        runTest {
            create()
            advanceUntilIdle()

            classToTest.download { _, _ -> Result.failure(Exception()) }

            assertThat(state.error).isEqualTo(Res.string.errorSomethingWentWrong)
            assertThat(state.notice).isNull()
        }

    @Test
    fun `delete fires deleted`() =
        runTest {
            wheneverBlocking { documents.delete("5") } doReturn Result.success(Unit)
            create()
            classToTest.askDelete()

            classToTest.deleted.test {
                classToTest.delete()
                advanceUntilIdle()
                awaitItem()
            }
            assertThat(state.confirmDelete).isFalse()
        }

    @Test
    fun `a failed delete closes the confirm with an error`() =
        runTest {
            wheneverBlocking {
                documents.delete(
                    "5",
                )
            } doReturn Result.failure(ApiException.ServerError(FAILED_TO_DELETE_DOCUMENT))
            create()
            advanceUntilIdle()
            classToTest.askDelete()

            classToTest.delete()
            advanceUntilIdle()

            assertThat(state.confirmDelete).isFalse()
            assertThat(state.isDeleting).isFalse()
            assertThat(state.error).isNotNull()
        }

    @Test
    fun `a doctor's upload belongs to that doctor's visit on its day`() =
        runTest {
            create(document.copy(creatorLabel = " aziz karimov ", date = "2026-09-20"))
            advanceUntilIdle()

            assertThat(state.appointment).isEqualTo(visit)
        }

    @Test
    fun `the backend's appointmentId wins over the guess`() =
        runTest {
            create(document.copy(creatorLabel = "Aziz Karimov", date = "2026-09-20", appointmentId = "43"))
            advanceUntilIdle()

            assertThat(state.appointment).isEqualTo(otherDay)
        }

    @Test
    fun `the patient's own upload has no visit`() =
        runTest {
            create(document.copy(date = "2026-09-20"))
            advanceUntilIdle()

            assertThat(state.appointment).isNull()
        }
}
