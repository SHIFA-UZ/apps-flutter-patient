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
import kotlinx.datetime.LocalDate
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
import shifa.composeapp.generated.resources.failedToUpdateProfile
import shifa.composeapp.generated.resources.invalidPhone
import shifa.composeapp.generated.resources.required
import shifa.uz.data.api.FAILED_TO_UPDATE_PROFILE
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.PatientProfile
import shifa.uz.data.model.ProfileUpdate
import shifa.uz.data.profile.ProfileRepository
import shifa.uz.data.storage.AppLanguage
import shifa.uz.data.storage.LanguageStorage
import shifa.uz.utils.ImagePickerResult

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val profile = mock<ProfileRepository>()
    private val languages = LanguageStorage(MapSettings(), "en")
    private val patient =
        PatientProfile(
            fullName = " Aziz Karimov Jr ",
            phone = "+998901234567",
            birthDate = "1990-05-12T00:00:00.000Z",
            gender = "female",
            address = "Tashkent",
            locationStreetAddress = "Amir Temur 1",
        )
    private lateinit var classToTest: EditProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun create(initial: PatientProfile? = patient) {
        classToTest = EditProfileViewModel(initial, profile, languages)
    }

    private val state get() = classToTest.state.value

    @Test
    fun `the form starts from the loaded profile`() {
        create()

        assertThat(state.firstName).isEqualTo("Aziz")
        assertThat(state.lastName).isEqualTo("Karimov Jr")
        assertThat(state.birthDate).isEqualTo(LocalDate(1990, 5, 12))
        assertThat(state.gender).isEqualTo(Gender.Female)
        assertThat(state.streetAddress).isEqualTo("Amir Temur 1")
        assertThat(state.language).isEqualTo(AppLanguage.English)
    }

    @Test
    fun `an unknown gender or date is left empty`() {
        create(PatientProfile(fullName = "Aziz", gender = "unknown", birthDate = "soon"))

        assertThat(state.gender).isNull()
        assertThat(state.birthDate).isNull()
        assertThat(state.lastName).isEmpty()
    }

    @Test
    fun `an invalid form shows its errors and isn't sent`() =
        runTest {
            create(null)
            classToTest.onPhoneChange("+998")

            classToTest.save()
            advanceUntilIdle()

            assertThat(state.showErrors).isTrue()
            assertThat(state.firstNameError).isEqualTo(Res.string.required)
            assertThat(state.phoneError).isEqualTo(Res.string.invalidPhone)
            verifyBlocking(profile, never()) { updateProfile(any()) }
        }

    @Test
    fun `save sends the fields without the photo and applies the language`() =
        runTest {
            val saved = patient.copy(fullName = "Aziz Karimov")
            wheneverBlocking { profile.updateProfile(any()) } doReturn Result.success(saved)
            create()
            classToTest.onLastNameChange(" Karimov ")
            classToTest.onAddressChange(" ")
            classToTest.onLanguageChange(AppLanguage.Russian)

            classToTest.events.test {
                classToTest.save()
                assertThat(state.isSaving).isTrue()
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(EditProfileEvent.Saved(saved))
            }
            verifyBlocking(profile) {
                updateProfile(
                    ProfileUpdate(
                        firstName = "Aziz",
                        lastName = "Karimov",
                        birthDate = "1990-05-12",
                        gender = "Female",
                        phone = "+998901234567",
                        address = null,
                        language = "ru",
                        locationStreetAddress = "Amir Temur 1",
                    ),
                )
            }
            assertThat(languages.language.value).isEqualTo(AppLanguage.Russian)
        }

    @Test
    fun `a failed save keeps the form and the language`() =
        runTest {
            wheneverBlocking { profile.updateProfile(any()) } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_UPDATE_PROFILE))
            create()
            classToTest.onLanguageChange(AppLanguage.German)

            classToTest.save()
            advanceUntilIdle()

            assertThat(state.isSaving).isFalse()
            assertThat(state.error).isEqualTo(Res.string.failedToUpdateProfile)
            assertThat(languages.language.value).isEqualTo(AppLanguage.English)
        }

    @Test
    fun `a new photo is saved right away`() =
        runTest {
            val updated = patient.copy(photoUrl = "https://cdn/p.jpg")
            wheneverBlocking { profile.changePhoto(any(), any()) } doReturn Result.success(updated)
            create()

            classToTest.events.test {
                classToTest.uploadPhoto(ImagePickerResult("content://1", "p.jpg", byteArrayOf(1)))
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(EditProfileEvent.PhotoChanged(updated))
            }
            assertThat(state.photoUrl).isEqualTo("https://cdn/p.jpg")
            assertThat(state.isUploading).isFalse()
        }
}
