package shifa.uz.ui.home

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class HomeScreenshotTest {
    @Test
    fun loaded() {
        captureRoboImage("screenshots/Home_loaded.png") { HomeLoadedPreview() }
    }

    @Test
    fun empty() {
        captureRoboImage("screenshots/Home_empty.png") { HomeEmptyPreview() }
    }

    @Test
    fun loading() {
        captureRoboImage("screenshots/Home_loading.png") { HomeLoadingPreview() }
    }

    @Test
    fun error() {
        captureRoboImage("screenshots/Home_error.png") { HomeErrorPreview() }
    }

    @Test
    fun upcoming() {
        captureRoboImage("screenshots/Upcoming_list.png") { UpcomingPreview() }
    }
}
