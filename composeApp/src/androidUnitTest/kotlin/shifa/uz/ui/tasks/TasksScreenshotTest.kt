package shifa.uz.ui.tasks

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class TasksScreenshotTest {
    @Test
    fun open() {
        captureRoboImage("screenshots/Tasks_open.png") { TasksOpenPreview() }
    }

    @Test
    fun completed() {
        captureRoboImage("screenshots/Tasks_completed.png") { TasksCompletedPreview() }
    }

    @Test
    fun checkInSelected() {
        captureRoboImage("screenshots/TaskCheckIn_selected.png") { TaskCheckInSelectedPreview() }
    }

    @Test
    fun checkInWaiting() {
        captureRoboImage("screenshots/TaskCheckIn_waiting.png") { TaskCheckInWaitingPreview() }
    }
}
