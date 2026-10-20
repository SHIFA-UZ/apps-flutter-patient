package shifa.uz.ui.messages

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class MessagesScreenshotTest {
    @Test
    fun list() {
        captureRoboImage("screenshots/Messages_list.png") { MessagesListPreview() }
    }

    @Test
    fun empty() {
        captureRoboImage("screenshots/Messages_empty.png") { MessagesEmptyPreview() }
    }

    @Test
    fun search() {
        captureRoboImage("screenshots/Messages_search.png") { MessagesSearchPreview() }
    }

    @Test
    fun conversation() {
        captureRoboImage("screenshots/Conversation_messages.png") { ConversationPreview() }
    }

    @Test
    fun newChat() {
        captureRoboImage("screenshots/Conversation_new.png") { ConversationNewPreview() }
    }

    @Test
    fun conversationRecording() {
        captureRoboImage("screenshots/Conversation_recording.png") { ConversationRecordingPreview() }
    }
}
