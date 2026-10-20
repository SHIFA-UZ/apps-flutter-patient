package shifa.uz.ui.register

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.codeSentAgain
import shifa.composeapp.generated.resources.emailRequired
import shifa.composeapp.generated.resources.emailRequiredForForeignPhone
import shifa.composeapp.generated.resources.errorRegistrationFailed
import shifa.composeapp.generated.resources.invalidVerificationCode
import shifa.composeapp.generated.resources.passwordRequirementUppercase
import shifa.composeapp.generated.resources.passwordsDoNotMatch
import shifa.composeapp.generated.resources.phoneOrEmailRequired
import shifa.composeapp.generated.resources.required
import shifa.uz.data.api.REGISTRATION_FAILED
import shifa.uz.data.api.SMS_COULD_NOT_BE_SENT
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CheckIdentifierResponse
import shifa.uz.data.model.OtpChannel
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.model.RegisterPatientRequest
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.utils.Country
import shifa.uz.utils.ImagePickerResult

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mock<AuthRepository>()
    private val profile = mock<ProfileRepository>()
    private lateinit var classToTest: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking {
            auth.checkIdentifier(
                anyOrNull(),
                anyOrNull(),
            )
        } doReturn Result.success(CheckIdentifierResponse())
        wheneverBlocking { auth.sendEmailOtp(any(), anyOrNull()) } doReturn Result.success(Unit)
        wheneverBlocking { auth.sendSmsOtp(any()) } doReturn Result.success(OtpChannel.Sms)
        wheneverBlocking { auth.registerPatient(any()) } doReturn Result.success(Unit)
        wheneverBlocking { auth.createPatientForDoctor(anyOrNull(), any(), any()) } doReturn Result.success(Unit)
        wheneverBlocking { profile.updateProfile(any()) } doReturn Result.success(PatientProfile())
        wheneverBlocking { profile.uploadPhoto(any(), any()) } doReturn Result.success("https://cdn/p.jpg")
        classToTest = RegisterViewModel(auth, profile)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    private fun fillAccount(
        phone: String = "90 123 45 67",
        email: String = "",
    ) = with(classToTest) {
        onFirstNameChange(" Ali ")
        onLastNameChange("Valiyev")
        onPhoneChange(phone)
        onEmailChange(email)
        onPasswordChange("Secret1!")
        onConfirmPasswordChange("Secret1!")
    }

    // Steps 1-3 with nothing on 2 and 3, ending on the code screen.
    private fun TestScope.reachCode(
        phone: String = "90 123 45 67",
        email: String = "",
    ) {
        fillAccount(phone, email)
        repeat(3) {
            classToTest.next()
            advanceTimeBy(1)
        }
        runCurrent()
    }

    @Test
    fun `next with empty fields shows the errors and calls nothing`() =
        runTest {
            classToTest.next()
            advanceUntilIdle()

            classToTest.state.test {
                val s = awaitItem()
                assertThat(s.showErrors).isTrue()
                assertThat(s.step).isEqualTo(RegisterStep.Account)
                assertThat(s.firstNameError).isEqualTo(Res.string.required)
                assertThat(s.phoneError).isEqualTo(Res.string.phoneOrEmailRequired)
            }
            verifyBlocking(auth, never()) { checkIdentifier(anyOrNull(), anyOrNull()) }
        }

    @Test
    fun `field rules follow the spec`() {
        classToTest.onFirstNameChange("   ")
        classToTest.onPhoneChange("+49 151 2345 6789")
        classToTest.onPasswordChange("secret1!")
        classToTest.onConfirmPasswordChange("secret1")

        assertThat(state.firstNameError).isEqualTo(Res.string.required)
        assertThat(state.phoneError).isEqualTo(Res.string.emailRequiredForForeignPhone)
        assertThat(state.passwordError).isEqualTo(Res.string.passwordRequirementUppercase)
        assertThat(state.confirmPasswordError).isEqualTo(Res.string.passwordsDoNotMatch)

        classToTest.onEmailChange("hans@praxis.solutions")
        assertThat(state.phoneError).isNull()
        assertThat(state.emailError).isNull()
    }

    @Test
    fun `a new account checks the normalized phone and email then moves to step 2`() =
        runTest {
            fillAccount(email = " a@shifa.uz ")
            classToTest.next()
            advanceUntilIdle()

            verifyBlocking(auth) { checkIdentifier("+998901234567", "a@shifa.uz") }
            assertThat(state.step).isEqualTo(RegisterStep.Info)
        }

    @Test
    fun `an existing patient gets the dialog and stays on step 1`() =
        runTest {
            wheneverBlocking { auth.checkIdentifier(anyOrNull(), anyOrNull()) } doReturn
                Result.success(CheckIdentifierResponse("patient"))
            fillAccount()
            classToTest.next()
            advanceUntilIdle()

            assertThat(state.dialog).isEqualTo(RegisterDialog.ExistingPatient)
            assertThat(state.step).isEqualTo(RegisterStep.Account)
        }

    @Test
    fun `an existing doctor goes straight to the email code and links a patient account`() =
        runTest {
            wheneverBlocking { auth.checkIdentifier(anyOrNull(), anyOrNull()) } doReturn
                Result.success(CheckIdentifierResponse("doctor", "Aziz", "Karimov"))
            fillAccount(email = "doc@shifa.uz")
            classToTest.next()
            advanceUntilIdle()
            assertThat(state.dialog).isEqualTo(RegisterDialog.ExistingDoctor("Aziz Karimov"))

            classToTest.onCreatePatientAccount()
            runCurrent()
            assertThat(state.step).isEqualTo(RegisterStep.Otp)
            assertThat(state.isDoctorLink).isTrue()
            verifyBlocking(auth) { sendEmailOtp("doc@shifa.uz") }

            classToTest.onOtpChange("123456")
            classToTest.next()
            runCurrent()
            verifyBlocking(auth) { createPatientForDoctor("+998901234567", "doc@shifa.uz", "123456") }
            verifyBlocking(auth, never()) { registerPatient(any()) }
            verifyBlocking(profile, never()) { updateProfile(any()) }
        }

    @Test
    fun `a doctor matched by phone only is asked for an email`() =
        runTest {
            wheneverBlocking { auth.checkIdentifier(anyOrNull(), anyOrNull()) } doReturn
                Result.success(CheckIdentifierResponse("doctor"))
            fillAccount()
            classToTest.next()
            advanceUntilIdle()

            classToTest.onCreatePatientAccount()
            advanceUntilIdle()

            assertThat(state.dialog).isNull()
            assertThat(state.error).isEqualTo(Res.string.emailRequired)
            verifyBlocking(auth, never()) { sendEmailOtp(any(), anyOrNull()) }
        }

    @Test
    fun `with an email the code goes by email and steps 2 and 3 are saved after register`() =
        runTest {
            val photo = ImagePickerResult("", "me.jpg", byteArrayOf(1, 2))
            fillAccount(email = "a@shifa.uz")
            classToTest.next()
            runCurrent()
            classToTest.onBirthDateChange(LocalDate(1990, 5, 1))
            classToTest.onCountryChange(Country("AE", "BAA", "United Arab Emirates"))
            classToTest.onAddressChange(" Tashkent ")
            classToTest.next()
            classToTest.onPhotoPicked(photo)
            classToTest.onPhotoPicked(null)
            classToTest.next()
            runCurrent()

            assertThat(state.step).isEqualTo(RegisterStep.Otp)
            assertThat(state.otpDestination).isEqualTo("a@shifa.uz")
            verifyBlocking(auth, never()) { sendSmsOtp(any()) }

            classToTest.registered.test {
                classToTest.onOtpChange("12 34 56")
                classToTest.next()
                runCurrent()
                awaitItem()
            }
            verifyBlocking(auth) {
                registerPatient(
                    RegisterPatientRequest(
                        "Ali",
                        "Valiyev",
                        "Secret1!",
                        "+998901234567",
                        "a@shifa.uz",
                        emailOtp = "123456",
                    ),
                )
            }
            verifyBlocking(profile) { uploadPhoto(photo.fileBytes, "me.jpg") }
            verifyBlocking(profile) {
                updateProfile(
                    ProfileUpdate(
                        birthDate = "1990-05-01",
                        address = "Tashkent",
                        locationCountry = "United Arab Emirates",
                        photoUrl = "https://cdn/p.jpg",
                    ),
                )
            }
            assertThat(state).isEqualTo(RegisterUiState())
        }

    @Test
    fun `without an email the code goes by SMS and nothing else is saved`() =
        runTest {
            reachCode()

            assertThat(state.otpChannel).isEqualTo(OtpChannel.Sms)
            assertThat(state.otpDestination).isEqualTo("+998 ** *** 4567")
            classToTest.onOtpChange("654321")
            classToTest.next()
            runCurrent()

            verifyBlocking(auth) {
                registerPatient(
                    RegisterPatientRequest("Ali", "Valiyev", "Secret1!", "+998901234567", smsOtp = "654321"),
                )
            }
            verifyBlocking(profile, never()) { updateProfile(any()) }
        }

    @Test
    fun `a failed SMS asks for an email and sends the code there`() =
        runTest {
            wheneverBlocking {
                auth.sendSmsOtp(
                    any(),
                )
            } doReturn Result.failure(ApiException.ServerError(SMS_COULD_NOT_BE_SENT))
            reachCode()
            assertThat(state.dialog).isEqualTo(RegisterDialog.EmailFallback)
            assertThat(state.step).isEqualTo(RegisterStep.Photo)

            classToTest.onFallbackEmailChange("nope")
            classToTest.onContinueWithEmail()
            assertThat(state.fallbackEmailInputError).isTrue()

            classToTest.onFallbackEmailChange(" A@Shifa.UZ ")
            classToTest.onContinueWithEmail()
            runCurrent()
            verifyBlocking(auth) { sendEmailOtp("a@shifa.uz") }
            assertThat(state.step).isEqualTo(RegisterStep.Otp)
            assertThat(state.otpDestination).isEqualTo("a@shifa.uz")

            classToTest.onOtpChange("111111")
            classToTest.next()
            runCurrent()
            verifyBlocking(auth) {
                registerPatient(
                    RegisterPatientRequest(
                        "Ali",
                        "Valiyev",
                        "Secret1!",
                        "+998901234567",
                        "a@shifa.uz",
                        emailOtp = "111111",
                    ),
                )
            }
        }

    @Test
    fun `the server switching the code to email also asks for one`() =
        runTest {
            wheneverBlocking { auth.sendSmsOtp(any()) } doReturn Result.success(OtpChannel.Email)
            reachCode()

            assertThat(state.dialog).isEqualTo(RegisterDialog.EmailFallback)
            assertThat(state.isLoading).isFalse()
        }

    @Test
    fun `resend waits out the cooldown and a fourth SMS asks for an email`() =
        runTest {
            reachCode()
            assertThat(state.resendSeconds).isEqualTo(RESEND_COOLDOWN_S)
            classToTest.resend()
            runCurrent()
            verifyBlocking(auth, times(1)) { sendSmsOtp(any()) }

            repeat(MAX_SMS_SENDS - 1) {
                advanceTimeBy(RESEND_COOLDOWN_S * 1_000L)
                runCurrent()
                assertThat(state.canResend).isTrue()
                classToTest.resend()
                runCurrent()
                assertThat(state.notice).isEqualTo(Res.string.codeSentAgain)
            }
            verifyBlocking(auth, times(MAX_SMS_SENDS)) { sendSmsOtp(any()) }

            advanceTimeBy(RESEND_COOLDOWN_S * 1_000L)
            runCurrent()
            classToTest.resend()
            assertThat(state.dialog).isEqualTo(RegisterDialog.EmailFallback)
            verifyBlocking(auth, times(MAX_SMS_SENDS)) { sendSmsOtp(any()) }
        }

    @Test
    fun `a short code is rejected without a request`() =
        runTest {
            reachCode()
            classToTest.onOtpChange("12a3")
            assertThat(state.otp).isEqualTo("123")

            classToTest.next()
            runCurrent()

            assertThat(state.error).isEqualTo(Res.string.invalidVerificationCode)
            verifyBlocking(auth, never()) { registerPatient(any()) }
        }

    @Test
    fun `a failed register shows the error on the code screen`() =
        runTest {
            wheneverBlocking {
                auth.registerPatient(
                    any(),
                )
            } doReturn Result.failure(ApiException.ServerError(REGISTRATION_FAILED))
            reachCode()
            classToTest.onOtpChange("123456")
            classToTest.next()
            runCurrent()

            assertThat(state.error).isEqualTo(Res.string.errorRegistrationFailed)
            assertThat(state.step).isEqualTo(RegisterStep.Otp)
            assertThat(state.isLoading).isFalse()
        }

    @Test
    fun `back walks the steps, clears the code and leaves from step 1`() =
        runTest {
            reachCode()
            classToTest.onOtpChange("123")

            assertThat(classToTest.back()).isTrue()
            assertThat(state.step).isEqualTo(RegisterStep.Photo)
            assertThat(state.otp).isEmpty()
            assertThat(state.resendSeconds).isEqualTo(0)
            assertThat(classToTest.back()).isTrue()
            assertThat(classToTest.back()).isTrue()
            assertThat(state.step).isEqualTo(RegisterStep.Account)
            assertThat(state.firstName).isEqualTo(" Ali ")
            assertThat(classToTest.back()).isFalse()

            classToTest.reset()
            assertThat(state).isEqualTo(RegisterUiState())
        }
}
