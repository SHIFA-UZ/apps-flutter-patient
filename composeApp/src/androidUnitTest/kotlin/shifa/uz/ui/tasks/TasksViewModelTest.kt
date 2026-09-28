package shifa.uz.ui.tasks

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInSubmittedSuccessfully
import shifa.composeapp.generated.resources.errorNetworkConnection
import shifa.uz.data.api.NETWORK_ERROR
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskProgress
import shifa.uz.data.tasks.TasksRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val tasks = mock<TasksRepository>()
    private val active = RemoteCareTask(id = "1", startDate = "2026-09-20")
    private val done = active.copy(id = "2", status = "COMPLETED")
    private val expiredDone = active.copy(id = "3", status = "EXPIRED", progress = TaskProgress(3, 3, 0))
    private val expiredLeft = active.copy(id = "4", status = "EXPIRED", progress = TaskProgress(3, 1, 2))
    private val cancelled = active.copy(id = "5", status = "CANCELLED")
    private val all = listOf(active, done, expiredDone, expiredLeft, cancelled)
    private val classToTest by lazy { TasksViewModel(tasks) }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { tasks.getMyTasks() } doReturn Result.success(all)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    @Test
    fun `open loads T1, and an EXPIRED task with nothing pending is in both tabs`() =
        runTest {
            classToTest.open()
            assertThat(state.isLoading).isTrue()
            advanceUntilIdle()

            assertThat(state.tasksIn(TasksTab.Open)).containsExactly(active, expiredDone, expiredLeft).inOrder()
            assertThat(state.tasksIn(TasksTab.Completed)).containsExactly(done, expiredDone).inOrder()
        }

    @Test
    fun `a failed reload keeps the list and shows the error`() =
        runTest {
            classToTest.open()
            advanceUntilIdle()
            wheneverBlocking { tasks.getMyTasks() } doReturn Result.failure(ApiException.NetworkError(NETWORK_ERROR))

            classToTest.load()
            advanceUntilIdle()

            assertThat(state.tasks).isEqualTo(all)
            assertThat(state.error).isEqualTo(Res.string.errorNetworkConnection)
        }

    @Test
    fun `a 401 signs out`() =
        runTest {
            wheneverBlocking { tasks.getMyTasks() } doReturn Result.failure(ApiException.Unauthorized("Unauthorized"))

            classToTest.signedOut.test {
                classToTest.open()
                advanceUntilIdle()
                awaitItem()
            }
        }

    @Test
    fun `a check-in reloads with the notice, and the next open clears it`() =
        runTest {
            classToTest.open()
            advanceUntilIdle()

            classToTest.onSubmitted()
            advanceUntilIdle()
            assertThat(state.notice).isEqualTo(Res.string.checkInSubmittedSuccessfully)
            verifyBlocking(tasks, times(2)) { getMyTasks() }

            classToTest.open()
            assertThat(state.notice).isNull()
        }
}
