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
class ShifaDropdownScreenshotTest {
    @Test
    fun `all states`() {
        captureRoboImage("screenshots/ShifaDropdown_all_states.png") { DropdownPreview() }
    }
}
