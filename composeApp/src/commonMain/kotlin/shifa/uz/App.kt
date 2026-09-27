package shifa.uz

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.accountDeletedSuccess
import shifa.uz.data.auth.AuthRepository
import shifa.uz.data.storage.languageStorage
import shifa.uz.ui.forgotpassword.ForgotPasswordScreen
import shifa.uz.ui.login.LoginScreen
import shifa.uz.ui.navigation.MainScreen
import shifa.uz.ui.register.RegisterScreen
import shifa.uz.ui.splash.SplashScreen
import shifa.uz.ui.theme.ShifaTheme
import shifa.uz.utils.ProvideAppLocale

private enum class Screen { Splash, Login, Register, ForgotPassword, Home }

@Composable
@Preview
fun App() {
    val language by languageStorage.language.collectAsState()
    ProvideAppLocale(language.code) {
        ShifaTheme {
            var screen by rememberSaveable { mutableStateOf(Screen.Splash) }
            // Login says so once after the account is deleted (14 §3.5).
            var accountDeleted by rememberSaveable { mutableStateOf(false) }
            val auth = remember { AuthRepository() }
            val scope = rememberCoroutineScope()

            fun show(next: Screen) {
                accountDeleted = false
                screen = next
            }
            Crossfade(screen) {
                when (it) {
                    Screen.Splash ->
                        SplashScreen(auth::restoreSession, onFinished = { signedIn ->
                            screen = if (signedIn) Screen.Home else Screen.Login
                        })
                    Screen.Login ->
                        LoginScreen(
                            onSignedIn = { show(Screen.Home) },
                            onForgotPassword = { show(Screen.ForgotPassword) },
                            onCreateAccount = { show(Screen.Register) },
                            notice = Res.string.accountDeletedSuccess.takeIf { accountDeleted },
                        )
                    Screen.Register ->
                        RegisterScreen(onRegistered = { screen = Screen.Home }, onClose = { screen = Screen.Login })
                    Screen.ForgotPassword ->
                        ForgotPasswordScreen(onSignedIn = { screen = Screen.Home }, onClose = { screen = Screen.Login })
                    Screen.Home ->
                        MainScreen(
                            // Awaited, so the token is gone before Login shows (Flutter didn't wait).
                            onLogOut = {
                                scope.launch {
                                    auth.logout()
                                    screen = Screen.Login
                                }
                            },
                            onAccountDeleted = {
                                accountDeleted = true
                                screen = Screen.Login
                            },
                        )
                }
            }
        }
    }
}
