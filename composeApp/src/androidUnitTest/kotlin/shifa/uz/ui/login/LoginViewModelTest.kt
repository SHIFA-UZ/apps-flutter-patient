package shifa.uz.ui.login

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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var classToTest: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        classToTest = LoginViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sign in with empty fields flags both`() =
        runTest {
            classToTest.signIn()
            advanceUntilIdle()

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.identifierError).isTrue()
                assertThat(state.passwordError).isTrue()
            }
        }

    @Test
    fun `blank identifier is an error but a blank password is not`() =
        runTest {
            classToTest.onIdentifierChange("   ")
            classToTest.onPasswordChange("   ")
            classToTest.signIn()
            advanceUntilIdle()

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.identifierError).isTrue()
                assertThat(state.passwordError).isFalse()
            }
        }

    @Test
    fun `sign in with both fields has no errors`() =
        runTest {
            classToTest.onIdentifierChange("patient@shifa.uz")
            classToTest.onPasswordChange("secret")
            classToTest.signIn()
            advanceUntilIdle()

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.identifierError).isFalse()
                assertThat(state.passwordError).isFalse()
            }
        }

    @Test
    fun `editing a field clears its error`() =
        runTest {
            classToTest.signIn()
            classToTest.onIdentifierChange("a")
            classToTest.onPasswordChange("b")
            advanceUntilIdle()

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.identifierError).isFalse()
                assertThat(state.passwordError).isFalse()
            }
        }

    @Test
    fun `toggle flips password visibility`() =
        runTest {
            classToTest.onTogglePasswordVisibility()
            advanceUntilIdle()

            classToTest.state.test {
                assertThat(awaitItem().isPasswordVisible).isTrue()
            }
        }

    @Test
    fun `banners and sign in follow the lockout state`() {
        assertThat(LoginUiState(loginAttemptsRemaining = 4).showAttemptsBanner).isTrue()
        assertThat(LoginUiState(loginAttemptsRemaining = 5).showAttemptsBanner).isFalse()
        assertThat(LoginUiState(loginAttemptsRemaining = 0).showAttemptsBanner).isFalse()

        val locked = LoginUiState(loginAttemptsRemaining = 2, loginLockedUntilMinutes = 3)
        assertThat(locked.showAttemptsBanner).isFalse()
        assertThat(locked.isSignInEnabled).isFalse()
        assertThat(LoginUiState(isLoading = true).isSignInEnabled).isFalse()
        assertThat(LoginUiState().isSignInEnabled).isTrue()
    }
}
