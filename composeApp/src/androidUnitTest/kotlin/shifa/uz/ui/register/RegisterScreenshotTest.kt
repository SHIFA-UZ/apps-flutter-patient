package shifa.uz.ui.register

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class RegisterScreenshotTest {
    @Test
    fun account() {
        captureRoboImage("screenshots/Register_account.png") { RegisterAccountPreview() }
    }

    @Test
    fun `account errors`() {
        captureRoboImage("screenshots/Register_account_errors.png") { RegisterAccountErrorsPreview() }
    }

    @Test
    fun info() {
        captureRoboImage("screenshots/Register_info.png") { RegisterInfoPreview() }
    }

    @Test
    fun photo() {
        captureRoboImage("screenshots/Register_photo.png") { RegisterPhotoPreview() }
    }

    @Test
    fun `sms code`() {
        captureRoboImage("screenshots/Register_sms_code.png") { RegisterSmsCodePreview() }
    }

    @Test
    fun `email code error`() {
        captureRoboImage("screenshots/Register_email_code_error.png") { RegisterEmailCodeErrorPreview() }
    }

    @Test
    fun `country tray`() {
        captureRoboImage("screenshots/Register_country_tray.png") { RegisterCountryTrayPreview() }
    }

    @Test
    @Config(qualifiers = "w402dp-h1600dp-xhdpi")
    fun dialogs() {
        captureRoboImage("screenshots/Register_dialogs.png") { RegisterDialogsPreview() }
    }
}
