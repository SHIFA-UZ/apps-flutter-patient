package shifa.uz.ui.tasks

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInNotesRequired
import shifa.composeapp.generated.resources.checkInPleaseSelectYesOrNo
import shifa.composeapp.generated.resources.checkInValueInvalidNumber
import shifa.composeapp.generated.resources.checkInValueRequired
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CheckInRequest
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskCheckIn
import shifa.uz.data.tasks.TasksRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCheckInViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val tasks = mock<TasksRepository>()

    // 14:00 in Tashkent.
    private val now = Instant.parse("2026-09-28T09:00:00Z")
    private val tashkent = TimeZone.of("Asia/Tashkent")
    private val numeric = RemoteCareTask(id = "7", startDate = "2026-09-20", inputType = "NUMERIC")
    private val past = TaskCheckIn(1, "2026-09-28", "13:00:00")
    private val soon = TaskCheckIn(2, "2026-09-28", "14:10:00")
    private val later = TaskCheckIn(3, "2026-09-28", "14:15:00")
    private val done = TaskCheckIn(4, "2026-09-28", "12:00:00", status = "COMPLETED")
    private val unparseable = TaskCheckIn(5, "tomorrow")
    private lateinit var classToTest: TaskCheckInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        wheneverBlocking { tasks.getCheckIns("7") } doReturn
            Result.success(listOf(past, soon, later, done, unparseable))
        wheneverBlocking { tasks.submitCheckIn(any(), any()) } doReturn Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = classToTest.state.value

    /** Opens the screen on slot 1, T3 loaded. */
    private fun TestScope.open(task: RemoteCareTask = numeric) {
        classToTest = TaskCheckInViewModel(task, tasks, { now }, tashkent)
        advanceUntilIdle()
        classToTest.select(1, true)
    }

    @Test
    fun `only PENDING slots due within 10 minutes are listed`() =
        runTest {
            open()

            assertThat(state.slots).containsExactly(past, soon).inOrder()
            assertThat(state.upcoming).containsExactly(later)
            assertThat(state.waiting).isTrue()
        }

    @Test
    fun `the next 3 PENDING slots not due yet are shown soonest first`() =
        runTest {
            val slots =
                listOf("2026-09-30", "2026-09-29", "2026-10-01", "2026-10-02").mapIndexed { i, day ->
                    TaskCheckIn(10L + i, day, "08:00:00")
                }
            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(slots + later + done)
            open()

            assertThat(state.slots).isEmpty()
            assertThat(state.upcoming).containsExactly(later, slots[1], slots[0]).inOrder()
        }

    @Test
    fun `with nothing PENDING it isn't waiting`() =
        runTest {
            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(listOf(done))
            open()

            assertThat(state.slots).isEmpty()
            assertThat(state.waiting).isFalse()
        }

    @Test
    fun `picking another slot starts an empty form, and unticking closes it`() =
        runTest {
            open()
            classToTest.onValueChange("12")
            classToTest.select(2, true)
            assertThat(state.selected).isEqualTo(2)
            assertThat(state.value).isEmpty()

            classToTest.select(2, false)
            assertThat(state.selected).isNull()
        }

    @Test
    fun `a reload keeps the selection while its slot is listed`() =
        runTest {
            open()
            classToTest.onValueChange("12")
            classToTest.load()
            advanceUntilIdle()
            assertThat(state.selected).isEqualTo(1)
            assertThat(state.value).isEqualTo("12")

            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(listOf(soon))
            classToTest.load()
            advanceUntilIdle()
            assertThat(state.selected).isNull()
            assertThat(state.value).isEmpty()
        }

    @Test
    fun `a number takes a comma and rejects text`() =
        runTest {
            open()
            classToTest.submit()
            assertThat(state.valueError).isEqualTo(Res.string.checkInValueRequired)

            classToTest.onValueChange("abc")
            assertThat(state.valueError).isNull()
            classToTest.submit()
            assertThat(state.valueError).isEqualTo(Res.string.checkInValueInvalidNumber)
            verifyBlocking(tasks, never()) { submitCheckIn(any(), any()) }

            classToTest.onValueChange(" 36,6 ")
            classToTest.submit()
            advanceUntilIdle()
            verifyBlocking(tasks) { submitCheckIn("7", CheckInRequest(1, numericValue = 36.6)) }
        }

    @Test
    fun `yes or no needs an answer`() =
        runTest {
            open(numeric.copy(inputType = "boolean"))
            classToTest.submit()
            assertThat(state.valueError).isEqualTo(Res.string.checkInPleaseSelectYesOrNo)

            classToTest.onAnswer(false)
            classToTest.submit()
            advanceUntilIdle()
            verifyBlocking(tasks) { submitCheckIn("7", CheckInRequest(1, booleanValue = false)) }
        }

    @Test
    fun `required notes can't be blank, and the text is sent trimmed`() =
        runTest {
            open(numeric.copy(inputType = "SCALE", notesRequired = true))
            classToTest.onValueChange(" fine ")
            classToTest.onNotesChange("  ")
            classToTest.submit()
            assertThat(state.notesError).isEqualTo(Res.string.checkInNotesRequired)

            classToTest.onNotesChange(" slept well ")
            classToTest.submitted.test {
                classToTest.submit()
                advanceUntilIdle()
                awaitItem()
            }
            verifyBlocking(tasks) { submitCheckIn("7", CheckInRequest(1, textValue = "fine", notes = "slept well")) }
        }

    @Test
    fun `a rejected check-in keeps the form and shows why`() =
        runTest {
            wheneverBlocking { tasks.submitCheckIn(any(), any()) } doReturn
                Result.failure(ApiException.ValidationError("Numeric value required"))
            open()
            classToTest.onValueChange("12")

            classToTest.submit()
            assertThat(state.isSubmitting).isTrue()
            advanceUntilIdle()

            assertThat(state.isSubmitting).isFalse()
            assertThat(state.error).isEqualTo(Res.string.checkInValueRequired)
            assertThat(state.value).isEqualTo("12")
        }

    @Test
    fun `a 401 on submit signs out`() =
        runTest {
            wheneverBlocking { tasks.submitCheckIn(any(), any()) } doReturn
                Result.failure(ApiException.Unauthorized("Unauthorized"))
            open()
            classToTest.onValueChange("12")

            classToTest.signedOut.test {
                classToTest.submit()
                advanceUntilIdle()
                awaitItem()
            }
        }
}
