package shifa.uz.ui.login

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
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorInvalidCredentials
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.uz.data.api.NETWORK_ERROR
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.auth.LoginAttempts
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.LoginResponse

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mock<AuthRepository>()
    private val attempts = LoginAttempts(MapSettings()) { 0L }
    private lateinit var classToTest: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        classToTest = LoginViewModel(repository, attempts)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun loginReturns(result: Result<LoginResponse>) {
        wheneverBlocking { repository.login(any(), any()) } doReturn result
    }

    private fun fillAndSignIn() {
        classToTest.onIdentifierChange("patient@shifa.uz")
        classToTest.onPasswordChange("secret")
        classToTest.signIn()
    }

    @Test
    fun `sign in with empty fields flags both and calls nothing`() =
        runTest {
            classToTest.signIn()
            advanceUntilIdle()

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.identifierError).isTrue()
                assertThat(state.passwordError).isTrue()
            }
            verifyBlocking(repository, never()) { login(any(), any()) }
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
    fun `success sends the normalized identifier, resets the form and signs in`() =
        runTest {
            loginReturns(Result.success(LoginResponse(token = "t")))
            classToTest.onIdentifierChange(" 90 123 45 67 ")
            classToTest.onPasswordChange(" secret ")

            classToTest.signedIn.test {
                classToTest.signIn()
                advanceUntilIdle()
                awaitItem()
            }
            verifyBlocking(repository) { login("+998901234567", " secret ") }
            assertThat(classToTest.state.value).isEqualTo(LoginUiState())
        }

    @Test
    fun `failure shows the error and the attempts left`() =
        runTest {
            loginReturns(Result.failure(ApiException.Unauthorized("Invalid credentials")))

            fillAndSignIn()
            advanceUntilIdle()

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.error).isEqualTo(Res.string.errorInvalidCredentials)
                assertThat(state.loginAttemptsRemaining).isEqualTo(4)
                assertThat(state.showAttemptsBanner).isTrue()
            }
        }

    @Test
    fun `editing a field clears the errors`() =
        runTest {
            loginReturns(Result.failure(ApiException.Unauthorized("Invalid credentials")))
            fillAndSignIn()
            advanceUntilIdle()
            classToTest.onPasswordChange("b")

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.error).isNull()
                assertThat(state.passwordError).isFalse()
            }
        }

    @Test
    fun `the fifth failure locks sign in`() =
        runTest {
            loginReturns(Result.failure(ApiException.Unauthorized("Invalid credentials")))

            repeat(5) {
                fillAndSignIn()
                advanceUntilIdle()
            }

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.loginLockedUntilMinutes).isEqualTo(5)
                assertThat(state.loginAttemptsRemaining).isEqualTo(0)
                assertThat(state.isSignInEnabled).isFalse()
            }
        }

    @Test
    fun `a lockout from an earlier session blocks the api call`() =
        runTest {
            repeat(5) { attempts.recordFailure() }
            classToTest = LoginViewModel(repository, attempts)

            fillAndSignIn()
            advanceUntilIdle()

            assertThat(classToTest.state.value.isLocked).isTrue()
            verifyBlocking(repository, never()) { login(any(), any()) }
        }

    @Test
    fun `a network error is shown but not counted`() =
        runTest {
            loginReturns(Result.failure(ApiException.NetworkError(NETWORK_ERROR)))

            fillAndSignIn()
            advanceUntilIdle()

            classToTest.state.test {
                val state = awaitItem()
                assertThat(state.error).isEqualTo(Res.string.errorNetworkConnection)
                assertThat(state.loginAttemptsRemaining).isEqualTo(5)
                assertThat(state.showAttemptsBanner).isFalse()
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
    fun `identifiers are normalized for the login call`() {
        assertThat(loginUsername(" Patient@Shifa.uz ")).isEqualTo("Patient@Shifa.uz")
        assertThat(loginUsername("901234567")).isEqualTo("+998901234567")
        assertThat(loginUsername("+998 90 123-45-67")).isEqualTo("+998901234567")
        assertThat(loginUsername("998901234567")).isEqualTo("+998901234567")
        assertThat(loginUsername("+49 151 2345678")).isEqualTo("+491512345678")
        assertThat(loginUsername("user42")).isEqualTo("user42")
        assertThat(loginUsername("john")).isEqualTo("john")
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
