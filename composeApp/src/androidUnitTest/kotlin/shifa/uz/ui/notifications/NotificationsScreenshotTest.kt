package shifa.uz.ui.notifications

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class NotificationsScreenshotTest {
    @Test
    fun inbox() {
        captureRoboImage("screenshots/Notifications_inbox.png") { NotificationsInboxPreview() }
    }

    @Test
    fun emptyFilter() {
        captureRoboImage("screenshots/Notifications_emptyFilter.png") { NotificationsEmptyFilterPreview() }
    }

    @Test
    fun appointmentTray() {
        captureRoboImage("screenshots/NotificationTray_appointment.png") { NotificationAppointmentTrayPreview() }
    }

    @Test
    fun documentTray() {
        captureRoboImage("screenshots/NotificationTray_document.png") { NotificationDocumentTrayPreview() }
    }

    @Test
    fun taskTray() {
        captureRoboImage("screenshots/NotificationTray_task.png") { NotificationTaskTrayPreview() }
    }
}
