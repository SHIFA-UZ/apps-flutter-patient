package shifa.uz.ui.shifaai

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.comingSoon
import shifa.composeapp.generated.resources.copilotDailyLimit
import shifa.composeapp.generated.resources.copilotTranscribeError
import shifa.composeapp.generated.resources.errorRecordingVoice
import shifa.uz.data.copilot.CopilotRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CopilotChat
import shifa.uz.data.model.CopilotMessage
import shifa.uz.data.model.CopilotMessage.Companion.ASSISTANT
import shifa.uz.data.model.CopilotMessage.Companion.USER
import shifa.uz.data.storage.AppLanguage
import shifa.uz.data.storage.CopilotStorage

@OptIn(ExperimentalCoroutinesApi::class)
class ShifaAiViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var reply = Result.success("Hello")
    private var gate: CompletableDeferred<Unit>? = null
    private val asked = mutableListOf<Pair<List<CopilotMessage>, String>>()

    // Streams "Hel", waits for [gate], then "lo", and returns [reply].
    private val copilot =
        object : CopilotRepository(mock<HttpClient>()) {
            override suspend fun stream(
                messages: List<CopilotMessage>,
                language: String,
                onToken: (String) -> Unit,
            ): Result<String> {
                asked += messages to language
                onToken("Hel")
                gate?.await()
                onToken("lo")
                return reply
            }

            override suspend fun transcribe(
                audio: ByteArray,
                language: String,
            ): Result<String> {
                heard += language
                return transcript
            }
        }
    private var transcript = Result.success("I have a headache")
    private val heard = mutableListOf<String>()
    private val storage = CopilotStorage(MapSettings())
    private var clock = 1000L
    private val saved =
        CopilotChat(
            "1",
            listOf(CopilotMessage(USER, "Can I take Paracetamol?"), CopilotMessage(ASSISTANT, "Usually yes.")),
            500,
        )
    private val classToTest by lazy {
        ShifaAiViewModel(
            copilot,
            storage,
            MutableStateFlow(AppLanguage.Russian),
        ) { clock }
    }

    private val state get() = classToTest.state.value

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        storage.save(listOf(saved))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a sent question streams its reply, then both are saved at the top`() =
        runTest {
            gate = CompletableDeferred()
            classToTest.open()
            classToTest.edit("  Headache?  ")

            classToTest.send()
            advanceUntilIdle()

            assertThat(state.input).isEmpty()
            assertThat(state.streaming).isEqualTo("Hel")
            assertThat(state.canSend).isFalse()
            assertThat(asked).containsExactly(listOf(CopilotMessage(USER, "Headache?")) to "RU")

            clock = 2000
            gate?.complete(Unit)
            advanceUntilIdle()

            val chat =
                CopilotChat("1000", listOf(CopilotMessage(USER, "Headache?"), CopilotMessage(ASSISTANT, "Hello")), 2000)
            assertThat(state.streaming).isNull()
            assertThat(state.chat).isEqualTo(chat)
            assertThat(state.chats).containsExactly(chat, saved).inOrder()
            assertThat(storage.load()).isEqualTo(state.chats)
        }

    @Test
    fun `an empty reply isn't a message, and a blank input isn't sent`() =
        runTest {
            reply = Result.success("")
            classToTest.open("1")
            classToTest.edit("   ")
            classToTest.send()
            classToTest.edit("And ibuprofen?")
            classToTest.send()
            advanceUntilIdle()

            assertThat(asked).hasSize(1)
            assertThat(
                state.chat?.messages?.map {
                    it.content
                },
            ).containsExactly("Can I take Paracetamol?", "Usually yes.", "And ibuprofen?").inOrder()
        }

    @Test
    fun `a rate limit keeps the chat shut until it's reopened`() =
        runTest {
            reply = Result.failure(ApiException.RateLimit("Too many requests"))
            classToTest.open("1")
            classToTest.edit("Hi")
            classToTest.send()
            advanceUntilIdle()

            assertThat(state.error).isEqualTo(Res.string.copilotDailyLimit)
            classToTest.edit("Again")
            assertThat(state.canSend).isFalse()
            classToTest.send()
            advanceUntilIdle()
            assertThat(asked).hasSize(1)
            // 09 §5.1: the question stays, unanswered.
            assertThat(state.chat?.messages?.last()).isEqualTo(CopilotMessage(USER, "Hi"))

            classToTest.close()
            classToTest.open("1")
            classToTest.edit("Again")
            assertThat(state.error).isNull()
            assertThat(state.canSend).isTrue()
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            reply = Result.failure(ApiException.Unauthorized("Unauthorized"))
            classToTest.open()
            classToTest.edit("Hi")

            classToTest.signedOut.test {
                classToTest.send()
                advanceUntilIdle()
                awaitItem()
            }
        }

    @Test
    fun `leaving stops the reply, and delete drops the chat from the device`() =
        runTest {
            gate = CompletableDeferred()
            classToTest.open("1")
            classToTest.edit("Hi")
            classToTest.send()
            advanceUntilIdle()

            classToTest.close()
            advanceUntilIdle()
            assertThat(state.chat).isNull()
            assertThat(state.streaming).isNull()
            assertThat(state.chats.single().messages.last()).isEqualTo(CopilotMessage(USER, "Hi"))

            classToTest.open("1")
            classToTest.delete()
            assertThat(state.chat).isNull()
            assertThat(state.chats).isEmpty()
            assertThat(storage.load()).isEmpty()
        }

    @Test
    fun `a new chat is only saved once it has a message, and search matches any turn ignoring case`() {
        classToTest.open()
        assertThat(state.chat).isEqualTo(CopilotChat("1000"))
        classToTest.showNotice(Res.string.comingSoon)
        assertThat(state.notice).isEqualTo(Res.string.comingSoon)
        classToTest.close()
        assertThat(state.chats).containsExactly(saved)

        classToTest.search(" PARACETAMOL ")
        assertThat(state.shown).containsExactly(saved)
        classToTest.search("ibuprofen")
        assertThat(state.shown).isEmpty()
    }

    @Test
    fun `a recording's transcript fills the input unsent, and a failure keeps the typed text`() =
        runTest {
            classToTest.open("1")
            classToTest.onRecording()
            assertThat(state.recording).isTrue()
            assertThat(state.canSend).isFalse()

            classToTest.transcribe(byteArrayOf(1))
            assertThat(state.isTranscribing).isTrue()
            advanceUntilIdle()
            assertThat(heard).containsExactly("ru")
            assertThat(state.input).isEqualTo("I have a headache")
            assertThat(state.chat?.messages).isEqualTo(saved.messages)
            assertThat(state.canSend).isTrue()

            transcript = Result.failure(ApiException.ServerError("x"))
            classToTest.transcribe(byteArrayOf(1))
            advanceUntilIdle()
            assertThat(state.input).isEqualTo("I have a headache")
            assertThat(state.error).isEqualTo(Res.string.copilotTranscribeError)

            classToTest.transcribe(null)
            assertThat(state.error).isEqualTo(Res.string.errorRecordingVoice)
            assertThat(heard).hasSize(2)
        }
}
