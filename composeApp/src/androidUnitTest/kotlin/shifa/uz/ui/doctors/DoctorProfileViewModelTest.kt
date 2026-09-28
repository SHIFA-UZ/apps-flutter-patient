package shifa.uz.ui.doctors

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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking
import shifa.uz.data.api.FAILED_TO_LOAD_DOCTOR
import shifa.uz.data.doctors.DoctorsRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Doctor

@OptIn(ExperimentalCoroutinesApi::class)
class DoctorProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val doctors = mock<DoctorsRepository>()
    private val card = Doctor(id = "d1", fullName = "Ali")
    private val full = card.copy(biography = "Cardiologist")
    private val classToTest by lazy { DoctorProfileViewModel(card, doctors) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { doctors.getDoctor("d1") } doReturn Result.success(full)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    @Test
    fun `it shows the card's doctor, then D2's`() =
        runTest {
            assertThat(state.doctor).isEqualTo(card)
            assertThat(state.isLoaded).isFalse()
            advanceUntilIdle()

            assertThat(state.doctor).isEqualTo(full)
            assertThat(state.isLoaded).isTrue()
        }

    @Test
    fun `a failed load shows the error and retries`() =
        runTest {
            wheneverBlocking {
                doctors.getDoctor(
                    "d1",
                )
            } doReturn Result.failure(ApiException.ServerError(FAILED_TO_LOAD_DOCTOR))
            assertThat(state.isLoading).isTrue()
            advanceUntilIdle()
            assertThat(state.error).isNotNull()
            assertThat(state.doctor).isEqualTo(card)

            wheneverBlocking { doctors.getDoctor("d1") } doReturn Result.success(full)
            classToTest.load()
            advanceUntilIdle()
            assertThat(state.error).isNull()
            assertThat(state.doctor).isEqualTo(full)
        }

    @Test
    fun `tabs switch`() {
        classToTest.selectTab(DoctorTab.Certificates)
        assertThat(state.tab).isEqualTo(DoctorTab.Certificates)
    }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking {
                doctors.getDoctor(
                    "d1",
                )
            } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))

            classToTest.signedOut.test {
                advanceUntilIdle()
                awaitItem()
            }
        }
}
