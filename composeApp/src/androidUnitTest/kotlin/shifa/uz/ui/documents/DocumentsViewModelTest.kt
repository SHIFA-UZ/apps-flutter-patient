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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.documentDeleted
import shifa.composeapp.generated.resources.documentUploadedSuccessfully
import shifa.composeapp.generated.resources.enterDocumentTitle
import shifa.composeapp.generated.resources.failedToDeleteDocument
import shifa.composeapp.generated.resources.uploadFailed
import shifa.uz.data.api.FAILED_TO_DELETE_DOCUMENT
import shifa.uz.data.api.FAILED_TO_UPLOAD_DOCUMENT
import shifa.uz.data.documents.DocumentsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.PatientDocument

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val documents = mock<DocumentsRepository>()
    private val blood =
        PatientDocument(id = "1", title = "Blood test", creatorLabel = "Patient", category = "BLOOD_TEST")
    private val mri = PatientDocument(id = "2", title = "MRI scan", creatorLabel = "Doctor", category = "MRI")
    private val classToTest by lazy { DocumentsViewModel(documents) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { documents.getDocuments() } doReturn Result.success(listOf(blood, mri))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    @Test
    fun `it loads the list and searches titles ignoring case`() =
        runTest {
            classToTest.load()
            assertThat(state.isLoading).isTrue()
            advanceUntilIdle()

            assertThat(state.documents).containsExactly(blood, mri).inOrder()
            classToTest.search("  mri ")
            assertThat(state.shown).containsExactly(mri)
        }

    @Test
    fun `a 401 on load signs out`() =
        runTest {
            wheneverBlocking { documents.getDocuments() } doReturn
                Result.failure(
                    ApiException.Unauthorized("Unauthorized"),
                )

            classToTest.signedOut.test {
                classToTest.load()
                advanceUntilIdle()
                awaitItem()
            }
            assertThat(state.error).isNotNull()
        }

    @Test
    fun `a picked file opens the popup titled after it, and Change keeps a typed title`() {
        classToTest.pick("scan.PDF", byteArrayOf(1))
        assertThat(state.draft?.title).isEqualTo("scan")

        classToTest.pick("other.jpg", byteArrayOf(2))
        assertThat(state.draft?.title).isEqualTo("other")

        classToTest.editTitle("My MRI")
        classToTest.pick("third.png", byteArrayOf(3))
        assertThat(state.draft?.fileName).isEqualTo("third.png")
        assertThat(state.draft?.title).isEqualTo("My MRI")
    }

    @Test
    fun `the same category again clears it`() {
        classToTest.pick("a.pdf", byteArrayOf(1))

        classToTest.selectCategory("MRI")
        assertThat(state.draft?.category).isEqualTo("MRI")
        classToTest.selectCategory("MRI")
        assertThat(state.draft?.category).isNull()
    }

    @Test
    fun `an empty title is not sent`() =
        runTest {
            classToTest.pick("a.pdf", byteArrayOf(1))
            classToTest.editTitle("  ")

            classToTest.upload()
            advanceUntilIdle()

            assertThat(state.draft?.titleMissing).isTrue()
            assertThat(state.draft?.error).isEqualTo(Res.string.enterDocumentTitle)
            verifyBlocking(documents, never()) { upload(any(), any(), any(), anyOrNull()) }
        }

    @Test
    fun `an upload puts the new document first and closes the popup`() =
        runTest {
            val new = PatientDocument(id = "3", title = "Scan")
            wheneverBlocking {
                documents.upload(
                    any(),
                    eq("a.pdf"),
                    eq("Scan"),
                    eq("MRI"),
                )
            } doReturn Result.success(new)
            classToTest.load()
            advanceUntilIdle()
            classToTest.pick("a.pdf", byteArrayOf(1))
            classToTest.editTitle(" Scan ")
            classToTest.selectCategory("MRI")

            classToTest.upload()
            assertThat(state.isUploading).isTrue()
            advanceUntilIdle()

            assertThat(state.draft).isNull()
            assertThat(state.documents).containsExactly(new, blood, mri).inOrder()
            assertThat(state.notice).isEqualTo(Res.string.documentUploadedSuccessfully)
        }

    @Test
    fun `a failed upload keeps the popup and the list`() =
        runTest {
            wheneverBlocking { documents.upload(any(), any(), any(), anyOrNull()) } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_UPLOAD_DOCUMENT))
            classToTest.load()
            advanceUntilIdle()
            classToTest.pick("a.pdf", byteArrayOf(1))

            classToTest.upload()
            advanceUntilIdle()

            assertThat(state.isUploading).isFalse()
            assertThat(state.draft?.error).isEqualTo(Res.string.uploadFailed)
            assertThat(state.documents).containsExactly(blood, mri).inOrder()
        }

    @Test
    fun `a deleted document leaves the list`() =
        runTest {
            classToTest.load()
            advanceUntilIdle()

            classToTest.onDeleted("1")

            assertThat(state.documents).containsExactly(mri)
            assertThat(state.notice).isEqualTo(Res.string.documentDeleted)
        }

    @Test
    fun `the filter narrows the list and only offers the categories in it`() =
        runTest {
            classToTest.load()
            advanceUntilIdle()
            assertThat(state.filters).containsExactly("BLOOD_TEST", "MRI").inOrder()

            classToTest.filterBy("MRI")
            assertThat(state.shown).containsExactly(mri)
            classToTest.filterBy(null)
            assertThat(state.shown).containsExactly(blood, mri).inOrder()
        }

    @Test
    fun `a swiped own upload is deleted with a notice`() =
        runTest {
            wheneverBlocking { documents.delete("1") } doReturn Result.success(Unit)
            classToTest.load()
            advanceUntilIdle()

            classToTest.delete(blood)
            assertThat(state.deleting).containsExactly("1")
            advanceUntilIdle()

            assertThat(state.documents).containsExactly(mri)
            assertThat(state.deleting).isEmpty()
            assertThat(state.notice).isEqualTo(Res.string.documentDeleted)
        }

    @Test
    fun `a failed swipe delete keeps the document and shows the error`() =
        runTest {
            wheneverBlocking { documents.delete("1") } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_DELETE_DOCUMENT))
            classToTest.load()
            advanceUntilIdle()

            classToTest.delete(blood)
            advanceUntilIdle()

            assertThat(state.documents).containsExactly(blood, mri).inOrder()
            assertThat(state.deleting).isEmpty()
            assertThat(state.error).isEqualTo(Res.string.failedToDeleteDocument)
        }

    @Test
    fun `a document without an id is not deleted`() =
        runTest {
            classToTest.delete(mri.copy(id = ""))
            advanceUntilIdle()

            verifyBlocking(documents, never()) { delete(any()) }
        }

    @Test
    fun `a 401 on delete signs out`() =
        runTest {
            wheneverBlocking {
                documents.delete(
                    "1",
                )
            } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))

            classToTest.signedOut.test {
                classToTest.delete(blood)
                advanceUntilIdle()
                awaitItem()
            }
        }
}
