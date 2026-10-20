package shifa.uz.ui.forgotpassword

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class ForgotPasswordScreenshotTest {
    @Test
    fun identifier() {
        captureRoboImage("screenshots/ForgotPassword_identifier.png") { ForgotPasswordIdentifierPreview() }
    }

    @Test
    fun `identifier error`() {
        captureRoboImage("screenshots/ForgotPassword_identifier_error.png") { ForgotPasswordIdentifierErrorPreview() }
    }

    @Test
    fun `sms code`() {
        captureRoboImage("screenshots/ForgotPassword_sms_code.png") { ForgotPasswordSmsCodePreview() }
    }

    @Test
    fun `email code`() {
        captureRoboImage("screenshots/ForgotPassword_email_code.png") { ForgotPasswordEmailCodePreview() }
    }

    @Test
    fun `new password`() {
        captureRoboImage("screenshots/ForgotPassword_new_password.png") { ForgotPasswordNewPasswordPreview() }
    }
}
