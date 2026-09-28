package shifa.uz.ui.shifaai

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class ShifaAiScreenshotTest {
    @Test
    fun chats() {
        captureRoboImage("screenshots/ShifaAi_chats.png") { ShifaAiChatsPreview() }
    }

    @Test
    fun noChats() {
        captureRoboImage("screenshots/ShifaAi_noChats.png") { ShifaAiNoChatsPreview() }
    }

    @Test
    fun chat() {
        captureRoboImage("screenshots/ShifaAiChat_messages.png") { ShifaAiChatPreview() }
    }

    @Test
    fun thinking() {
        captureRoboImage("screenshots/ShifaAiChat_thinking.png") { ShifaAiThinkingPreview() }
    }

    @Test
    fun rateLimit() {
        captureRoboImage("screenshots/ShifaAiChat_rateLimit.png") { ShifaAiRateLimitPreview() }
    }

    @Test
    fun addToChat() {
        captureRoboImage("screenshots/ShifaAiChat_addToChat.png") { ShifaAiAddToChatPreview() }
    }
}
