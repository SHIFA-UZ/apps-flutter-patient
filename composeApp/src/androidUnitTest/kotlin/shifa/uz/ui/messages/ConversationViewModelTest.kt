package shifa.uz.ui.messages

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorRecordingVoice
import shifa.composeapp.generated.resources.messageStillSending
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.RecordingTracker
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChatMessage
import shifa.uz.data.model.Conversation
import shifa.uz.data.model.MessageType
import shifa.uz.data.model.SendMessageRequest

// The poll never ends: it runs in backgroundScope and time moves a poll at a time.
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val chat = mock<ChatRepository>()
    private val hello = ChatMessage("1", text = "Hello", created = "2026-09-28T08:00:00Z")
    private val reply = ChatMessage("2", text = "Hi", created = "2026-09-28T08:01:00Z")
    private var target = ChatTarget("3", name = "Aziz")
    private var now = 1_000_000_000L
    private val tracker = RecordingTracker()
    private val classToTest by lazy {
        ConversationViewModel(target, chat, pollMs = 5_000, analytics = tracker) { Instant.fromEpochMilliseconds(now) }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking {
            chat.conversation(
                any(),
                anyOrNull(),
            )
        } doReturn Result.success(Conversation(null, listOf(hello)))
        wheneverBlocking { chat.markRead(any()) } doReturn Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the chat and sending a text are tracked without the text`() =
        runTest {
            wheneverBlocking { chat.send(any()) } doReturn Result.success(null)
            open()
            classToTest.edit("Hello doctor")
            classToTest.send()
            advanceUntilIdle()

            assertThat(tracker.events.first()).isEqualTo(AnalyticsEvent.ChatOpened(source = null, isNew = false))
            val sent = tracker.events.filterIsInstance<AnalyticsEvent.ChatMessageSent>().single()
            assertThat(sent.type).isEqualTo("text")
            assertThat(sent.isFirstInChat).isFalse()
            assertThat(sent.properties.values).doesNotContain("Hello doctor")
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
            verifyBlocking(chat) { conversation("3", null) }

            // A backend that ignores `after` sends everything again: same list.
            nextPoll()
            verifyBlocking(chat) { conversation("3", "1") }
            verifyBlocking(chat, times(1)) { markRead("3") }

            wheneverBlocking {
                chat.conversation(
                    any(),
                    anyOrNull(),
                )
            } doReturn Result.success(Conversation(null, listOf(hello, reply)))
            nextPoll()
            assertThat(state.messages).containsExactly(hello, reply).inOrder()
            verifyBlocking(chat, times(2)) { markRead("3") }
        }

    @Test
    fun `polls ask only for messages after the newest and merge them`() =
        runTest {
            open()
            wheneverBlocking { chat.conversation("3", "1") } doReturn Result.success(Conversation(null, listOf(reply)))
            nextPoll()
            assertThat(state.messages).containsExactly(hello, reply).inOrder()

            wheneverBlocking { chat.conversation("3", "2") } doReturn Result.success(Conversation(null, emptyList()))
            nextPoll()
            verifyBlocking(chat) { conversation("3", "2") }
            assertThat(state.messages).containsExactly(hello, reply).inOrder()
        }

    @Test
    fun `a full page of newer messages is read on from its newest`() =
        runTest {
            open()
            // Ids 2..101, a second apart after "Hello" (08:00).
            val page =
                (2..101).map {
                    val t = it - 1
                    ChatMessage(
                        "$it",
                        text = "m$it",
                        created = "2026-09-28T08:0${1 + t / 60}:${(t % 60).toString().padStart(2, '0')}Z",
                    )
                }
            wheneverBlocking { chat.conversation("3", "1") } doReturn Result.success(Conversation(null, page))
            wheneverBlocking { chat.conversation("3", "101") } doReturn Result.success(Conversation(null, emptyList()))
            nextPoll()
            verifyBlocking(chat) { conversation("3", "101") }
            assertThat(state.messages).hasSize(101)
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
            verifyBlocking(chat, never()) { conversation(any(), anyOrNull()) }

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
            verifyBlocking(chat) { conversation("7", null) }
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
            classToTest.attach("IMG_1.jpg", byteArrayOf(1))
            assertThat(state.isSending).isTrue()
            classToTest.attach("IMG_2.jpg", byteArrayOf(2)) // a second one meanwhile says so
            assertThat(state.error).isEqualTo(Res.string.messageStillSending)
            runCurrent()

            verifyBlocking(chat) {
                send(SendMessageRequest(3, null, "My rash", "image", "https://f/p.jpg", "IMG_1.jpg", fileSize = 1))
            }
            assertThat(state.isSending).isFalse()
            assertThat(state.input).isEmpty()
        }

    @Test
    fun `a recording is uploaded and sent as a voice message with its length, a file as a document`() =
        runTest {
            wheneverBlocking { chat.upload(any(), any()) } doReturn Result.success("https://f/v.m4a")
            wheneverBlocking { chat.send(any()) } doReturn Result.success(null)
            open()

            classToTest.onRecording()
            assertThat(state.recording).isTrue()
            now += 7_000
            classToTest.sendVoice(byteArrayOf(1, 2, 3))
            assertThat(state.recording).isFalse()
            runCurrent()
            verifyBlocking(chat) {
                send(
                    SendMessageRequest(
                        3,
                        null,
                        null,
                        "voice",
                        "https://f/v.m4a",
                        "voice_1000007000.m4a",
                        fileSize = 3,
                        duration = 7,
                    ),
                )
            }

            classToTest.sendVoice(null)
            assertThat(state.error).isEqualTo(Res.string.errorRecordingVoice)

            classToTest.attach("scan.pdf", byteArrayOf(1), MessageType.Document)
            runCurrent()
            verifyBlocking(chat) {
                send(SendMessageRequest(3, null, null, "document", "https://f/v.m4a", "scan.pdf", fileSize = 1))
            }
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking {
                chat.conversation(
                    any(),
                    anyOrNull(),
                )
            } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))

            classToTest.signedOut.test {
                open()
                awaitItem()
            }
        }

    @Test
    fun `after a failed poll the next one waits twice as long`() =
        runTest {
            wheneverBlocking {
                chat.conversation(
                    any(),
                    anyOrNull(),
                )
            } doReturn Result.failure(ApiException.NetworkError(""))
            backgroundScope.launch { classToTest.poll() }
            runCurrent()
            verifyBlocking(chat, times(1)) { conversation(any(), anyOrNull()) }

            advanceTimeBy(5_100) // the usual 5 s: not yet
            verifyBlocking(chat, times(1)) { conversation(any(), anyOrNull()) }
            advanceTimeBy(5_000)
            verifyBlocking(chat, times(2)) { conversation(any(), anyOrNull()) }
        }
}
