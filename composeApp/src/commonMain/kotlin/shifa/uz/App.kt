package shifa.uz

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.ui.tooling.preview.Preview
import shifa.uz.data.auth.AuthRepository
import shifa.uz.ui.forgotpassword.ForgotPasswordScreen
import shifa.uz.ui.home.HomeScreen
import shifa.uz.ui.login.LoginScreen
import shifa.uz.ui.register.RegisterScreen
import shifa.uz.ui.splash.SplashScreen
import shifa.uz.ui.theme.ShifaTheme

private enum class Screen { Splash, Login, Register, ForgotPassword, Home }

@Composable
@Preview
fun App() {
    ShifaTheme {
        var screen by rememberSaveable { mutableStateOf(Screen.Splash) }
        val auth = remember { AuthRepository() }
        Crossfade(screen) {
            when (it) {
                Screen.Splash ->
                    SplashScreen(auth::restoreSession, onFinished = { signedIn ->
                        screen = if (signedIn) Screen.Home else Screen.Login
                    })
                Screen.Login ->
                    LoginScreen(
                        onSignedIn = { screen = Screen.Home },
                        onForgotPassword = { screen = Screen.ForgotPassword },
                        onCreateAccount = { screen = Screen.Register },
                    )
                Screen.Register ->
                    RegisterScreen(onRegistered = { screen = Screen.Home }, onClose = { screen = Screen.Login })
                Screen.ForgotPassword ->
                    ForgotPasswordScreen(onSignedIn = { screen = Screen.Home }, onClose = { screen = Screen.Login })
                Screen.Home ->
                    HomeScreen(onLogOut = {
                        auth.logout()
                        screen = Screen.Login
                    })
            }
        }
    }
}
