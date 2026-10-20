package shifa.uz.ui.widgets

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w402dp-h1800dp-xhdpi")
class ShifaCardsScreenshotTest {
    @Test
    fun `all cards`() {
        captureRoboImage("screenshots/ShifaCards_all_cards.png") { CardsPreview() }
    }
}
