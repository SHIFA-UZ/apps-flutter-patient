package shifa.uz.ui.visit

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
class VisitScreenshotTest {
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent")) // visit times are local
    }

    @Test
    fun live() {
        captureRoboImage("screenshots/LiveAppointment_sign.png") { LiveAppointmentPreview() }
    }

    @Test
    fun liveVideo() {
        captureRoboImage("screenshots/LiveAppointment_video.png") { LiveAppointmentVideoPreview() }
    }

    @Test
    fun signTray() {
        captureRoboImage("screenshots/LiveAppointment_signTray.png") { SignTrayPreview() }
    }

    @Test
    fun join() {
        captureRoboImage("screenshots/VideoCall_join.png") { VideoCallJoinPreview() }
    }

    @Test
    fun waiting() {
        captureRoboImage("screenshots/VideoCall_waiting.png") { VideoCallWaitingPreview() }
    }
}
