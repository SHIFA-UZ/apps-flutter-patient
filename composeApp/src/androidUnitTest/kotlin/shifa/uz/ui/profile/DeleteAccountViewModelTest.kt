package shifa.uz.ui.profile

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.errorSomethingWentWrong
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.uz.data.api.FAILED_TO_DELETE_ACCOUNT
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.DeleteAccountChallenge
import shifa.uz.data.profile.ProfileRepository

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val profile = mock<ProfileRepository>()
    private val auth = mock<AuthRepository>()
    private val challenge = DeleteAccountChallenge("c1", expiresInSeconds = 3)
    private lateinit var classToTest: DeleteAccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { auth.sendEmailOtp(any(), anyOrNull()) } doReturn Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create() {
        classToTest = DeleteAccountViewModel(challenge, "aziz@example.com", profile, auth)
    }

    private val state get() = classToTest.state.value

    @Test
    fun `opening sends the code and counts down the challenge's lifetime`() =
        runTest {
            create()
            runCurrent()

            assertThat(state.resendSeconds).isEqualTo(3)
            assertThat(state.canResend).isFalse()
            verifyBlocking(auth) { sendEmailOtp("aziz@example.com", "ACCOUNT_DELETION") }

            advanceUntilIdle()
            assertThat(state.resendSeconds).isEqualTo(0)
            assertThat(state.notice).isNull()
        }

    @Test
    fun `resend waits for the countdown`() =
        runTest {
            create()
            runCurrent()

            classToTest.resend()
            runCurrent()
            verifyBlocking(auth, times(1)) { sendEmailOtp(any(), anyOrNull()) }

            advanceTimeBy(3_001)
            classToTest.resend()
            runCurrent()
            verifyBlocking(auth, times(2)) { sendEmailOtp(any(), anyOrNull()) }
            assertThat(state.notice).isEqualTo(Res.string.codeSentAgain)
        }

    @Test
    fun `a short code isn't sent`() =
        runTest {
            create()
            classToTest.onOtpChange("12a3")

            classToTest.confirm()
            advanceUntilIdle()

            assertThat(state.otp).isEqualTo("123")
            assertThat(state.error).isEqualTo(Res.string.invalidVerificationCode)
            verifyBlocking(profile, never()) { confirmDeletion(any(), any(), any()) }
        }

    @Test
    fun `a confirmed deletion signs out, then leaves`() =
        runTest {
            wheneverBlocking { profile.confirmDeletion(any(), any(), any()) } doReturn Result.success(Unit)
            create()
            classToTest.onOtpChange("123456")

            classToTest.deleted.test {
                classToTest.confirm()
                advanceUntilIdle()
                awaitItem()
            }
            inOrder(profile, auth) {
                verifyBlocking(profile) { confirmDeletion("c1", "aziz@example.com", "123456") }
                verifyBlocking(auth) { logout() }
            }
        }

    @Test
    fun `a failed deletion keeps the session`() =
        runTest {
            wheneverBlocking { profile.confirmDeletion(any(), any(), any()) } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_DELETE_ACCOUNT))
            create()
            classToTest.onOtpChange("123456")

            classToTest.confirm()
            advanceUntilIdle()

            assertThat(state.isDeleting).isFalse()
            assertThat(state.error).isEqualTo(Res.string.errorSomethingWentWrong)
            verifyBlocking(auth, never()) { logout() }
        }
}
