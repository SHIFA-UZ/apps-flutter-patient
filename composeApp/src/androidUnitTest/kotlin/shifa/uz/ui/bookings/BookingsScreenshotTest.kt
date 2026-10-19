package shifa.uz.ui.bookings

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
class BookingsScreenshotTest {
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent")) // times are local
    }

    @Test
    fun loaded() {
        captureRoboImage("screenshots/Bookings_loaded.png") { BookingsLoadedPreview() }
    }

    @Test
    fun filtered() {
        captureRoboImage("screenshots/Bookings_filtered.png") { BookingsFilteredPreview() }
    }

    @Test
    fun myDoctors() {
        captureRoboImage("screenshots/Bookings_myDoctors.png") { BookingsMyDoctorsPreview() }
    }

    @Test
    fun past() {
        captureRoboImage("screenshots/Bookings_past.png") { BookingsPastPreview() }
    }

    @Test
    fun recommended() {
        captureRoboImage("screenshots/Bookings_recommended.png") { BookingsRecommendedPreview() }
    }

    @Test
    fun loading() {
        captureRoboImage("screenshots/Bookings_loading.png") { BookingsLoadingPreview() }
    }

    @Test
    fun detailsSoon() {
        captureRoboImage("screenshots/AppointmentDetails_soon.png") { AppointmentDetailsSoonPreview() }
    }

    @Test
    fun detailsCancellable() {
        captureRoboImage("screenshots/AppointmentDetails_cancellable.png") { AppointmentDetailsCancellablePreview() }
    }

    @Test
    fun detailsPast() {
        captureRoboImage("screenshots/AppointmentDetails_past.png") { AppointmentDetailsPastPreview() }
    }

    @Test
    fun detailsReviewed() {
        captureRoboImage("screenshots/AppointmentDetails_reviewed.png") { AppointmentDetailsReviewedPreview() }
    }

    @Test
    fun reviewTray() {
        captureRoboImage("screenshots/AppointmentDetails_reviewTray.png") { ReviewTrayPreview() }
    }
}
