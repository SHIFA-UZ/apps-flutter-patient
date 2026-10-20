package shifa.uz.ui.messages

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.failedToStartChat
import shifa.uz.data.chat.ChatRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.ChatContact
import shifa.uz.data.model.ChatUser

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val chat = mock<ChatRepository>()
    private val aziz = ChatContact("3", "11", "Aziz", lastAt = "2026-09-27T08:00:00Z")
    private val malika = ChatContact("4", "12", "Malika", lastAt = "2026-09-28T08:00:00Z")
    private val classToTest by lazy { MessagesViewModel(chat, debounceMs = 400) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { chat.conversations() } doReturn Result.success(listOf(aziz, malika))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    @Test
    fun `it loads the chats newest first`() =
        runTest {
            classToTest.load()
            assertThat(state.isLoading).isTrue()
            advanceUntilIdle()

            assertThat(state.isLoading).isFalse()
            assertThat(state.sorted).containsExactly(malika, aziz).inOrder()
        }

    @Test
    fun `the search waits for a pause and only the last query is sent`() =
        runTest {
            val found = listOf(ChatUser("11", "Aziz"))
            wheneverBlocking { chat.search("Az") } doReturn Result.success(found)

            classToTest.search("A")
            advanceTimeBy(100)
            classToTest.search(" Az ")
            advanceUntilIdle()

            verifyBlocking(chat, never()) { search("A") }
            assertThat(state.results).isEqualTo(found)
            assertThat(state.isSearching).isFalse()
            classToTest.search("")
            assertThat(state.isSearch).isFalse()
            assertThat(state.results).isNull()
        }

    @Test
    fun `a doctor with a chat opens it, one without opens a new chat`() =
        runTest {
            classToTest.open.test {
                classToTest.start(ChatUser("11", "Aziz"))
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(ChatTarget("3", null, "Aziz"))

                classToTest.start(ChatUser("15", "Kamola", "https://p/k.jpg"))
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(ChatTarget(null, "15", "Kamola", "https://p/k.jpg"))
            }
        }

    @Test
    fun `a failed start shows its error and opens nothing`() =
        runTest {
            wheneverBlocking { chat.conversations() } doReturn Result.failure(ApiException.ServerError("x"))

            classToTest.start(ChatUser("11", "Aziz"))
            advanceUntilIdle()

            assertThat(state.error).isEqualTo(Res.string.failedToStartChat)
            assertThat(state.isStarting).isFalse()
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking { chat.conversations() } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))

            classToTest.signedOut.test {
                classToTest.load()
                advanceUntilIdle()
                awaitItem()
            }
        }
}
