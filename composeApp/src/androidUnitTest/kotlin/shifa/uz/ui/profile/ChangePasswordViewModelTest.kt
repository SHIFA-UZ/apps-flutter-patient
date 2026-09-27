package shifa.uz.ui.profile

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
import shifa.composeapp.generated.resources.errorFailedToChangePassword
import shifa.composeapp.generated.resources.passwordRequired
import shifa.uz.data.api.FAILED_TO_CHANGE_PASSWORD
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.model.ApiException

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mock<AuthRepository>()
    private lateinit var classToTest: ChangePasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        classToTest = ChangePasswordViewModel(auth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    private fun fill() {
        classToTest.onCurrentPasswordChange("OldSecret1!")
        classToTest.onNewPasswordChange("Secret1!")
        classToTest.onConfirmPasswordChange("Secret1!")
    }

    @Test
    fun `an invalid form shows its errors and isn't sent`() =
        runTest {
            classToTest.onNewPasswordChange("Secret1!")
            classToTest.onConfirmPasswordChange("Secret2!")

            classToTest.save()
            advanceUntilIdle()

            assertThat(state.showErrors).isTrue()
            assertThat(state.currentPasswordError).isEqualTo(Res.string.passwordRequired)
            assertThat(state.confirmPasswordError).isNotNull()
            verifyBlocking(auth, never()) { changePassword(any(), any()) }
        }

    @Test
    fun `a valid form changes the password`() =
        runTest {
            wheneverBlocking { auth.changePassword(any(), any()) } doReturn Result.success(Unit)
            fill()

            classToTest.changed.test {
                classToTest.save()
                assertThat(state.isSaving).isTrue()
                advanceUntilIdle()
                awaitItem()
            }
            verifyBlocking(auth) { changePassword("OldSecret1!", "Secret1!") }
        }

    @Test
    fun `a failure is shown and the form can be sent again`() =
        runTest {
            wheneverBlocking { auth.changePassword(any(), any()) } doReturn
                Result.failure(ApiException.ServerError(FAILED_TO_CHANGE_PASSWORD))
            fill()

            classToTest.save()
            advanceUntilIdle()

            assertThat(state.isSaving).isFalse()
            assertThat(state.error).isEqualTo(Res.string.errorFailedToChangePassword)
        }
}
