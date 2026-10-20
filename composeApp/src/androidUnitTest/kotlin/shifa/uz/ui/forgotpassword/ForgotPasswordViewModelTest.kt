package shifa.uz.ui.forgotpassword

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.emailOrPhoneRequired
import shifa.composeapp.generated.resources.errorFailedToResetPassword
import shifa.composeapp.generated.resources.invalidPhone
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.composeapp.generated.resources.passwordsDoNotMatch
import shifa.uz.data.api.FAILED_TO_RESET_PASSWORD
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.OtpChannel

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mock<AuthRepository>()
    private lateinit var classToTest: ForgotPasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { auth.sendForgotPasswordOtp(any()) } doReturn Result.success(OtpChannel.Email)
        wheneverBlocking { auth.resetForgottenPassword(any(), any(), any(), any()) } doReturn Result.success(Unit)
        classToTest = ForgotPasswordViewModel(auth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    private fun TestScope.reachCode(identifier: String = " Ali@Shifa.UZ ") {
        classToTest.onIdentifierChange(identifier)
        classToTest.next()
        runCurrent()
    }

    private fun TestScope.reachNewPassword(identifier: String = " Ali@Shifa.UZ ") {
        reachCode(identifier)
        classToTest.onOtpChange("123456")
        classToTest.next()
        runCurrent()
    }

    @Test
    fun `an empty or broken identifier sends nothing`() =
        runTest {
            classToTest.next()
            assertThat(state.identifierError).isEqualTo(Res.string.emailOrPhoneRequired)
            assertThat(state.showErrors).isTrue()

            classToTest.onIdentifierChange("ali99")
            classToTest.next()
            runCurrent()
            assertThat(state.identifierError).isEqualTo(Res.string.invalidPhone)
            verifyBlocking(auth, never()) { sendForgotPasswordOtp(any()) }
        }

    @Test
    fun `the email is lowercased and a local phone gets +998`() =
        runTest {
            reachCode()
            verifyBlocking(auth) { sendForgotPasswordOtp("ali@shifa.uz") }
            assertThat(state.step).isEqualTo(ForgotPasswordStep.Code)
            assertThat(state.showErrors).isFalse()

            classToTest.back()
            reachCode("90 123 45 67")
            verifyBlocking(auth) { sendForgotPasswordOtp("+998901234567") }
        }

    @Test
    fun `the code screen counts down and resend waits a minute`() =
        runTest {
            reachCode()
            assertThat(state.expiresInSeconds).isEqualTo(FORGOT_CODE_EXPIRY_S)
            assertThat(state.resendSeconds).isEqualTo(FORGOT_RESEND_COOLDOWN_S)
            classToTest.resend()
            runCurrent()
            verifyBlocking(auth, times(1)) { sendForgotPasswordOtp(any()) }

            advanceTimeBy(FORGOT_RESEND_COOLDOWN_S * 1_000L)
            runCurrent()
            assertThat(state.canResend).isTrue()
            assertThat(state.expiresInSeconds).isEqualTo(FORGOT_CODE_EXPIRY_S - FORGOT_RESEND_COOLDOWN_S)
            classToTest.resend()
            runCurrent()
            verifyBlocking(auth, times(2)) { sendForgotPasswordOtp(any()) }
            assertThat(state.notice).isEqualTo(Res.string.codeSentAgain)
            assertThat(state.expiresInSeconds).isEqualTo(FORGOT_CODE_EXPIRY_S)
        }

    @Test
    fun `a short code stays on the code screen and nothing is reset`() =
        runTest {
            reachCode()
            classToTest.onOtpChange("12a3")
            classToTest.next()

            assertThat(state.otp).isEqualTo("123")
            assertThat(state.error).isEqualTo(Res.string.invalidVerificationCode)
            assertThat(state.step).isEqualTo(ForgotPasswordStep.Code)
        }

    @Test
    fun `a valid new password resets with the code and signs in`() =
        runTest {
            wheneverBlocking { auth.sendForgotPasswordOtp(any()) } doReturn Result.success(OtpChannel.Sms)
            reachNewPassword("901234567")
            assertThat(state.step).isEqualTo(ForgotPasswordStep.NewPassword)

            classToTest.onPasswordChange("Secret1!")
            classToTest.onConfirmPasswordChange("Secret1")
            classToTest.next()
            assertThat(state.confirmPasswordError).isEqualTo(Res.string.passwordsDoNotMatch)
            verifyBlocking(auth, never()) { resetForgottenPassword(any(), any(), any(), any()) }

            classToTest.onConfirmPasswordChange("Secret1!")
            classToTest.signedIn.test {
                classToTest.next()
                runCurrent()
                awaitItem()
            }
            verifyBlocking(auth) { resetForgottenPassword("+998901234567", OtpChannel.Sms, "123456", "Secret1!") }
            assertThat(state).isEqualTo(ForgotPasswordUiState())
        }

    @Test
    fun `a rejected code goes back to the code screen`() =
        runTest {
            wheneverBlocking { auth.resetForgottenPassword(any(), any(), any(), any()) } doReturn
                Result.failure(ApiException.ValidationError("Invalid or expired verification code"))
            reachNewPassword()
            classToTest.onPasswordChange("Secret1!")
            classToTest.onConfirmPasswordChange("Secret1!")
            classToTest.next()
            runCurrent()

            assertThat(state.step).isEqualTo(ForgotPasswordStep.Code)
            assertThat(state.error).isEqualTo(Res.string.invalidVerificationCode)
            assertThat(state.otp).isEmpty()
            assertThat(state.password).isEqualTo("Secret1!")
        }

    @Test
    fun `other reset failures stay on the new password screen`() =
        runTest {
            wheneverBlocking { auth.resetForgottenPassword(any(), any(), any(), any()) } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_RESET_PASSWORD))
            reachNewPassword()
            classToTest.onPasswordChange("Secret1!")
            classToTest.onConfirmPasswordChange("Secret1!")
            classToTest.next()
            runCurrent()

            assertThat(state.step).isEqualTo(ForgotPasswordStep.NewPassword)
            assertThat(state.error).isEqualTo(Res.string.errorFailedToResetPassword)
            assertThat(state.isLoading).isFalse()
        }

    @Test
    fun `back walks the steps and leaves from the first`() =
        runTest {
            reachNewPassword()

            assertThat(classToTest.back()).isTrue()
            assertThat(state.step).isEqualTo(ForgotPasswordStep.Code)
            assertThat(state.otp).isEqualTo("123456")
            assertThat(classToTest.back()).isTrue()
            assertThat(state).isEqualTo(ForgotPasswordUiState(identifier = " Ali@Shifa.UZ "))
            assertThat(classToTest.back()).isFalse()
        }
}
