package shifa.uz

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.ui.tooling.preview.Preview
import shifa.uz.ui.login.LoginScreen
import shifa.uz.ui.splash.SplashScreen
import shifa.uz.ui.theme.ShifaTheme

private enum class Screen { Splash, Login }

@Composable
@Preview
fun App() {
    ShifaTheme {
        var screen by rememberSaveable { mutableStateOf(Screen.Splash) }
        Crossfade(screen) {
            when (it) {
                Screen.Splash -> SplashScreen(onFinished = { screen = Screen.Login })
                // /forgot-password and /create-account are not built yet (AUTH-06, AUTH-03).
                Screen.Login -> LoginScreen(onForgotPassword = {}, onCreateAccount = {})
            }
        }
    }
}
