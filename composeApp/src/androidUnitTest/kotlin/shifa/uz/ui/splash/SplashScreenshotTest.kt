package shifa.uz.ui.splash

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class SplashScreenshotTest {
    @Test
    fun start() {
        captureRoboImage("screenshots/Splash_start.png") { SplashStartPreview() }
    }

    @Test
    fun middle() {
        captureRoboImage("screenshots/Splash_middle.png") { SplashMiddlePreview() }
    }

    @Test
    fun end() {
        captureRoboImage("screenshots/Splash_end.png") { SplashEndPreview() }
    }
}
