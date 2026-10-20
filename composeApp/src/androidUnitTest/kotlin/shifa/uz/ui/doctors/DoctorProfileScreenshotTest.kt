package shifa.uz.ui.doctors

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class DoctorProfileScreenshotTest {
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent")) // slot times are local
    }

    @Test
    fun about() {
        captureRoboImage("screenshots/DoctorProfile_about.png") { DoctorProfileAboutPreview() }
    }

    @Test
    fun services() {
        captureRoboImage("screenshots/DoctorProfile_services.png") { DoctorProfileServicesPreview() }
    }

    @Test
    fun clinic() {
        captureRoboImage("screenshots/DoctorProfile_clinic.png") { DoctorProfileClinicPreview() }
    }

    @Test
    fun error() {
        captureRoboImage("screenshots/DoctorProfile_error.png") { DoctorProfileErrorPreview() }
    }

    @Test
    fun reviews() {
        captureRoboImage("screenshots/DoctorProfile_reviews.png") { DoctorProfileReviewsPreview() }
    }
}
