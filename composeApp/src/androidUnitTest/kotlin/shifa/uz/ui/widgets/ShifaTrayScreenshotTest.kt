package shifa.uz.ui.widgets

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w402dp-h1600dp-xhdpi")
class ShifaTrayScreenshotTest {
    @Test
    fun `all types`() {
        captureRoboImage("screenshots/ShifaTray_all_types.png") { TrayPreview() }
    }
}
