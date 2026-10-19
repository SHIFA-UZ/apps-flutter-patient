package shifa.uz.ui.debug

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class DebugScreenshotTest {
    @Test
    fun menu() {
        captureRoboImage("screenshots/Debug_menu.png") { DebugPreview() }
    }
}
