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
class CreateBookingScreenshotTest {
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent")) // slot times are local
    }

    @Test
    fun incomplete() {
        captureRoboImage("screenshots/CreateBooking_incomplete.png") { CreateBookingIncompletePreview() }
    }

    @Config(qualifiers = "w393dp-h1000dp-xhdpi")
    @Test
    fun complete() {
        captureRoboImage("screenshots/CreateBooking_complete.png") { CreateBookingCompletePreview() }
    }

    @Test
    @Config(qualifiers = "w393dp-h1000dp-xhdpi")
    fun reschedule() {
        captureRoboImage("screenshots/Reschedule.png") { ReschedulePreview() }
    }

    @Test
    @Config(qualifiers = "w393dp-h1100dp-xhdpi")
    fun payNow() {
        captureRoboImage("screenshots/CreateBooking_payNow.png") { CreateBookingPayNowPreview() }
    }

    @Test
    @Config(qualifiers = "w393dp-h1000dp-xhdpi")
    fun payInClinic() {
        captureRoboImage("screenshots/CreateBooking_payInClinic.png") { CreateBookingPayInClinicPreview() }
    }

    @Test
    fun confirmPayment() {
        captureRoboImage("screenshots/CreateBooking_confirmPayment.png") { ConfirmPaymentTrayPreview() }
    }

    @Test
    fun payment() {
        captureRoboImage("screenshots/Payment.png") { PaymentPreview() }
    }

    @Test
    fun paymentExpired() {
        captureRoboImage("screenshots/Payment_expired.png") { PaymentExpiredPreview() }
    }
}
