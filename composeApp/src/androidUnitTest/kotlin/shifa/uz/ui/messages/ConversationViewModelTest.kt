package shifa.uz.ui.messages

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChatMessage
import shifa.uz.data.model.Conversation
import shifa.uz.data.model.SendMessageRequest

// The poll never ends: it runs in backgroundScope and time moves a poll at a time.
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val chat = mock<ChatRepository>()
    private val hello = ChatMessage("1", text = "Hello", created = "2026-09-28T08:00:00Z")
    private val reply = ChatMessage("2", text = "Hi", created = "2026-09-28T08:01:00Z")
    private var target = ChatTarget("3", name = "Aziz")
    private val classToTest by lazy { ConversationViewModel(target, chat, pollMs = 5_000) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { chat.conversation(any()) } doReturn Result.success(Conversation(null, listOf(hello)))
        wheneverBlocking { chat.markRead(any()) } doReturn Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    private fun TestScope.open() {
        backgroundScope.launch { classToTest.poll() }
        runCurrent()
    }

    private fun TestScope.nextPoll() {
        advanceTimeBy(5_000)
        runCurrent()
    }

    @Test
    fun `it loads on open, polls and marks read only when something new comes`() =
        runTest {
            open()
            assertThat(state.messages).containsExactly(hello)
            verifyBlocking(chat) { markRead("3") }

            nextPoll()
            verifyBlocking(chat, times(2)) { conversation("3") }
            verifyBlocking(chat, times(1)) { markRead("3") }

            wheneverBlocking {
                chat.conversation(
                    any(),
                )
            } doReturn Result.success(Conversation(null, listOf(hello, reply)))
            nextPoll()
            assertThat(state.messages).containsExactly(hello, reply).inOrder()
            verifyBlocking(chat, times(2)) { markRead("3") }
        }

    @Test
    fun `a new chat waits for its first message, then switches to the chat it made`() =
        runTest {
            target = ChatTarget(null, "15", "Kamola")
            val echo =
                ChatMessage("9", chatId = "7", text = "Hi doctor", created = "2026-09-28T08:00:00Z", isMine = true)
            wheneverBlocking { chat.send(any()) } doReturn Result.success(echo)
            open()
            assertThat(state.messages).isEmpty()
            verifyBlocking(chat, never()) { conversation(any()) }

            classToTest.edit(" Hi doctor ")
            classToTest.send()
            runCurrent()

            verifyBlocking(chat) { send(SendMessageRequest(recipientDoctorId = 15, text = "Hi doctor")) }
            assertThat(state.conversationId).isEqualTo("7")
            assertThat(state.doctorId).isNull()
            assertThat(state.input).isEmpty()
            assertThat(state.messages).containsExactly(echo)
            // Kept until a fetch has it.
            nextPoll()
            verifyBlocking(chat) { conversation("7") }
            assertThat(state.messages).containsExactly(hello, echo).inOrder()
        }

    @Test
    fun `a failed send keeps the text`() =
        runTest {
            wheneverBlocking { chat.send(any()) } doReturn Result.failure(ApiException.ServerError("x"))
            open()

            classToTest.edit("Hello")
            classToTest.send()
            runCurrent()

            assertThat(state.input).isEqualTo("Hello")
            assertThat(state.error).isNotNull()
            assertThat(state.canSend).isTrue()
        }

    @Test
    fun `a photo is uploaded, then sent with the text as its caption`() =
        runTest {
            wheneverBlocking { chat.upload(any(), any()) } doReturn Result.success("https://f/p.jpg")
            wheneverBlocking { chat.send(any()) } doReturn Result.success(null)
            open()

            classToTest.edit("My rash")
            classToTest.pick("IMG_1.jpg", byteArrayOf(1))
            assertThat(state.isSending).isTrue()
            runCurrent()

            verifyBlocking(chat) {
                send(SendMessageRequest(3, null, "My rash", "image", "https://f/p.jpg", "IMG_1.jpg"))
            }
            assertThat(state.isSending).isFalse()
            assertThat(state.input).isEmpty()
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking {
                chat.conversation(
                    any(),
                )
            } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))

            classToTest.signedOut.test {
                open()
                awaitItem()
            }
        }
}
