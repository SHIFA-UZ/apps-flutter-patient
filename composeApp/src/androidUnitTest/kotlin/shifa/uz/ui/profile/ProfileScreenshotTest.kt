package shifa.uz.ui.profile

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class ProfileScreenshotTest {
    @Test
    fun loaded() {
        captureRoboImage("screenshots/Profile_loaded.png") { ProfileLoadedPreview() }
    }

    @Test
    fun notice() {
        captureRoboImage("screenshots/Profile_notice.png") { ProfileNoticePreview() }
    }

    @Test
    fun error() {
        captureRoboImage("screenshots/Profile_error.png") { ProfileErrorPreview() }
    }

    @Test
    fun dialogs() {
        captureRoboImage("screenshots/Profile_dialogs.png") { ProfileDialogsPreview() }
    }

    @Test
    fun `edit profile`() {
        captureRoboImage("screenshots/EditProfile.png") { EditProfilePreview() }
    }

    @Test
    fun `edit profile errors`() {
        captureRoboImage("screenshots/EditProfile_errors.png") { EditProfileErrorsPreview() }
    }

    @Test
    fun `change password`() {
        captureRoboImage("screenshots/ChangePassword.png") { ChangePasswordPreview() }
    }

    @Test
    fun `delete account`() {
        captureRoboImage("screenshots/DeleteAccount.png") { DeleteAccountPreview() }
    }
}
