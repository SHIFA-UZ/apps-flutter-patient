package shifa.uz.ui.profile

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
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
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.deleteAccountNoChannel
import shifa.composeapp.generated.resources.errorSomethingWentWrong
import shifa.composeapp.generated.resources.failedToUploadPhoto
import shifa.composeapp.generated.resources.languageChanged
import shifa.composeapp.generated.resources.profilePhotoUpdatedSuccessfully
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsSettings
import shifa.uz.data.analytics.RecordingTracker
import shifa.uz.data.api.FAILED_TO_UPLOAD_PHOTO
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.DeleteAccountChallenge
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.storage.AppLanguage
import shifa.uz.data.storage.LanguageStorage
import shifa.uz.utils.ImagePickerResult

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val profile = mock<ProfileRepository>()
    private val languages = LanguageStorage(MapSettings(), "en")
    private val analyticsSettings = AnalyticsSettings(MapSettings())
    private val tracker = RecordingTracker()
    private val optOuts = mutableListOf<Pair<Boolean, PatientProfile?>>()
    private val identified = mutableListOf<PatientProfile>()
    private val patient = PatientProfile(fullName = "Aziz Karimov", email = "aziz@example.com", language = "ru")
    private lateinit var classToTest: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { profile.getProfile() } doReturn Result.success(patient)
        wheneverBlocking { profile.updateProfile(any()) } doReturn Result.success(patient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create() {
        classToTest =
            ProfileViewModel(profile, languages, analyticsSettings, tracker, { out, p -> optOuts += out to p }) {
                identified += it
            }
    }

    private val state get() = classToTest.state.value

    @Test
    fun `it loads the profile and keeps the app language over the server's`() =
        runTest {
            create()
            assertThat(state.isLoading).isTrue()
            advanceUntilIdle()

            assertThat(state.isLoading).isFalse()
            assertThat(state.profile).isEqualTo(patient)
            assertThat(state.language).isEqualTo(AppLanguage.English)
        }

    @Test
    fun `a new language re-identifies the patient so the person's language follows`() =
        runTest {
            create()
            advanceUntilIdle()

            classToTest.selectLanguage(AppLanguage.Uzbek)

            assertThat(identified).containsExactly(patient)
        }

    @Test
    fun `turning share usage data off sends the opt-out event last, then saves and opts the SDK out`() =
        runTest {
            create()
            advanceUntilIdle()
            assertThat(state.shareUsageData).isTrue()

            classToTest.setShareUsageData(false)

            assertThat(state.shareUsageData).isFalse()
            assertThat(analyticsSettings.optedOut).isTrue()
            assertThat(tracker.events).containsExactly(AnalyticsEvent.AnalyticsOptedOut)
            assertThat(optOuts).containsExactly(true to null)
        }

    @Test
    fun `turning share usage data back on opts in with the profile, then sends the opt-in event`() =
        runTest {
            analyticsSettings.optedOut = true
            create()
            advanceUntilIdle()
            assertThat(state.shareUsageData).isFalse()

            classToTest.setShareUsageData(true)
            classToTest.setShareUsageData(true) // no change: nothing more

            assertThat(analyticsSettings.optedOut).isFalse()
            assertThat(optOuts).containsExactly(false to patient)
            assertThat(tracker.events).containsExactly(AnalyticsEvent.AnalyticsOptedIn)
        }

    @Test
    fun `a 401 on load signs out`() =
        runTest {
            wheneverBlocking { profile.getProfile() } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))
            create()

            classToTest.events.test {
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(ProfileEvent.SignedOut)
            }
            assertThat(state.error).isEqualTo(Res.string.errorSomethingWentWrong)
        }

    @Test
    fun `picking a language switches the app and tells the backend`() =
        runTest {
            create()
            advanceUntilIdle()
            classToTest.showDialog(ProfileDialog.Language)

            classToTest.selectLanguage(AppLanguage.Uzbek)
            advanceUntilIdle()

            assertThat(languages.language.value).isEqualTo(AppLanguage.Uzbek)
            assertThat(state.language).isEqualTo(AppLanguage.Uzbek)
            assertThat(state.dialog).isNull()
            assertThat(state.notice).isEqualTo(Res.string.languageChanged)
            verifyBlocking(profile) { updateProfile(ProfileUpdate(language = "uz")) }
        }

    @Test
    fun `a new photo is uploaded, saved and shown`() =
        runTest {
            val updated = patient.copy(photoUrl = "https://cdn/p.jpg")
            wheneverBlocking { profile.changePhoto(any(), any()) } doReturn Result.success(updated)
            create()
            advanceUntilIdle()

            classToTest.events.test {
                classToTest.uploadPhoto(ImagePickerResult("content://1", "p.jpg", byteArrayOf(1)))
                assertThat(state.isUploading).isTrue()
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(ProfileEvent.Changed)
            }
            assertThat(state.isUploading).isFalse()
            assertThat(state.profile).isEqualTo(updated)
            assertThat(state.notice).isEqualTo(Res.string.profilePhotoUpdatedSuccessfully)
        }

    @Test
    fun `a cancelled pick does nothing and a failed upload says so`() =
        runTest {
            wheneverBlocking { profile.changePhoto(any(), any()) } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_UPLOAD_PHOTO))
            create()
            advanceUntilIdle()

            classToTest.uploadPhoto(null)
            assertThat(state.isUploading).isFalse()

            classToTest.uploadPhoto(ImagePickerResult("content://1", "p.jpg", byteArrayOf(1)))
            advanceUntilIdle()
            assertThat(state.isUploading).isFalse()
            assertThat(state.error).isEqualTo(Res.string.failedToUploadPhoto)
        }

    @Test
    fun `an account that can get no code is told so instead of opening the code page`() =
        runTest {
            wheneverBlocking { profile.getProfile() } doReturn Result.success(patient.copy(email = " "))
            wheneverBlocking { profile.requestDeletion() } doReturn
                Result.success(DeleteAccountChallenge("c1", channels = emptyList()))
            create()
            advanceUntilIdle()
            classToTest.showDialog(ProfileDialog.DeleteAccount)

            classToTest.requestDeletion()
            advanceUntilIdle()

            assertThat(state.dialog).isNull()
            assertThat(state.deletion).isNull()
            assertThat(state.error).isEqualTo(Res.string.deleteAccountNoChannel)
        }

    @Test
    fun `an account without an email opens the code page when the backend offers SMS`() =
        runTest {
            val challenge = DeleteAccountChallenge("c1", channels = listOf("SMS"))
            wheneverBlocking { profile.getProfile() } doReturn
                Result.success(patient.copy(email = null, phone = "+998901234567"))
            wheneverBlocking { profile.requestDeletion() } doReturn Result.success(challenge)
            create()
            advanceUntilIdle()

            classToTest.requestDeletion()
            advanceUntilIdle()

            assertThat(state.deletion).isEqualTo(challenge)
            assertThat(state.error).isNull()
        }

    @Test
    fun `a deletion challenge opens the code page, and the dialog stays while it's requested`() =
        runTest {
            val challenge = DeleteAccountChallenge("c1", expiresInSeconds = 120)
            wheneverBlocking { profile.requestDeletion() } doReturn Result.success(challenge)
            create()
            advanceUntilIdle()
            classToTest.showDialog(ProfileDialog.DeleteAccount)

            classToTest.requestDeletion()
            classToTest.dismissDialog()
            assertThat(state.dialog).isEqualTo(ProfileDialog.DeleteAccount)
            assertThat(state.isRequestingDeletion).isTrue()
            advanceUntilIdle()

            assertThat(state.dialog).isNull()
            assertThat(state.isRequestingDeletion).isFalse()
            assertThat(state.deletion).isEqualTo(challenge)
        }
}
