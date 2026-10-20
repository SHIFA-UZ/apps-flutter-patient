package shifa.uz.ui.login

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class LoginScreenshotTest {
    @Test
    fun default() {
        captureRoboImage("screenshots/Login_default.png") { LoginDefaultPreview() }
    }

    @Test
    fun filled() {
        captureRoboImage("screenshots/Login_filled.png") { LoginFilledPreview() }
    }

    @Test
    fun errors() {
        captureRoboImage("screenshots/Login_errors.png") { LoginErrorsPreview() }
    }

    @Test
    fun `failed sign in`() {
        captureRoboImage("screenshots/Login_failed.png") { LoginFailedPreview() }
    }

    @Test
    fun `attempts banner`() {
        captureRoboImage("screenshots/Login_attempts.png") { LoginAttemptsPreview() }
    }

    @Test
    fun `locked banner`() {
        captureRoboImage("screenshots/Login_locked.png") { LoginLockedPreview() }
    }
}
