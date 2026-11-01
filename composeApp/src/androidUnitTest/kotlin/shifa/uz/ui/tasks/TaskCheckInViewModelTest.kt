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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInNotesRequired
import shifa.composeapp.generated.resources.checkInPleaseSelectYesOrNo
import shifa.composeapp.generated.resources.checkInSubmittedSuccessfully
import shifa.composeapp.generated.resources.checkInValueInvalidNumber
import shifa.composeapp.generated.resources.checkInValueRequired
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CheckInRequest
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskCheckIn
import shifa.uz.data.model.TaskProgress
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

    /** Opens the screen on the 28th, T3 loaded: its first PENDING check-in is [past]. */
    private fun TestScope.open(task: RemoteCareTask = numeric) {
        classToTest = TaskCheckInViewModel(task, tasks, { now }, tashkent)
        advanceUntilIdle()
        classToTest.openDay(LocalDate(2026, 9, 28))
    }

    @Test
    fun `a day opens its first PENDING check-in, due from 10 minutes before`() =
        runTest {
            open()
            assertThat(state.entry).isEqualTo(past)
            assertThat(state.entryDue).isTrue()

            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(listOf(later, done))
            classToTest.load()
            advanceUntilIdle()
            classToTest.openDay(LocalDate(2026, 9, 28))
            assertThat(state.entry).isEqualTo(later)
            assertThat(state.entryDue).isFalse()
            classToTest.onValueChange("12")
            classToTest.submit()
            verifyBlocking(tasks, never()) { submitCheckIn(any(), any()) }
        }

    @Test
    fun `a day with only submitted check-ins shows them, and one with nothing doesn't open`() =
        runTest {
            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(listOf(done))
            open()
            assertThat(state.day).isEqualTo(LocalDate(2026, 9, 28))
            assertThat(state.entry).isNull()
            assertThat(state.submitted).containsExactly(done)

            classToTest.closeDay()
            classToTest.openDay(LocalDate(2026, 9, 29))
            assertThat(state.day).isNull()
        }

    @Test
    fun `the calendar opens on this month, or the task's months, and progress comes from T3`() =
        runTest {
            wheneverBlocking { tasks.getCheckIns("7") } doReturn
                Result.success(listOf(TaskCheckIn(9, "2026-11-02", "08:00:00"), done))
            classToTest = TaskCheckInViewModel(numeric, tasks, { now }, tashkent)
            assertThat(state.month).isEqualTo(LocalDate(2026, 9, 1))
            advanceUntilIdle()

            assertThat(state.month).isEqualTo(LocalDate(2026, 9, 1))
            assertThat(state.lastMonth).isEqualTo(LocalDate(2026, 11, 1))
            assertThat(state.shownTask.progress).isEqualTo(TaskProgress(2, 1, 1))

            wheneverBlocking {
                tasks.getCheckIns(
                    "7",
                )
            } doReturn Result.success(listOf(done.copy(scheduledDate = "2026-07-30")))
            classToTest = TaskCheckInViewModel(numeric, tasks, { now }, tashkent)
            advanceUntilIdle()
            assertThat(state.month).isEqualTo(LocalDate(2026, 7, 1))
        }

    @Test
    fun `a submit closes the day with a notice and reloads`() =
        runTest {
            open()
            classToTest.onValueChange("12")
            classToTest.submitted.test {
                classToTest.submit()
                advanceUntilIdle()
                awaitItem()
            }
            assertThat(state.day).isNull()
            assertThat(state.notice).isEqualTo(Res.string.checkInSubmittedSuccessfully)
            verifyBlocking(tasks, times(2)) { getCheckIns("7") }
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

    @Test
    fun `a notification's Submit Check in opens today's check-in, or says nothing is due`() =
        runTest {
            val next = TaskCheckIn(6, "2026-09-30", "09:00:00")
            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(listOf(done, next, soon))
            classToTest = TaskCheckInViewModel(numeric, tasks, { now }, tashkent, openDue = true)
            advanceUntilIdle()
            assertThat(state.day).isEqualTo(LocalDate(2026, 9, 28))
            assertThat(state.entry).isEqualTo(soon)

            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(listOf(done))
            classToTest = TaskCheckInViewModel(numeric, tasks, { now }, tashkent, openDue = true)
            classToTest.nothingDue.test {
                advanceUntilIdle()
                awaitItem()
            }
            assertThat(state.day).isNull()
        }

    @Test
    fun `with today finished, a notification doesn't open another day's check-ins`() =
        runTest {
            val tomorrow = TaskCheckIn(6, "2026-09-29", "05:00:00")
            val yesterday = TaskCheckIn(7, "2026-09-27", "05:00:00")
            wheneverBlocking { tasks.getCheckIns("7") } doReturn Result.success(listOf(yesterday, done, tomorrow))
            classToTest = TaskCheckInViewModel(numeric, tasks, { now }, tashkent, openDue = true)
            classToTest.nothingDue.test {
                advanceUntilIdle()
                awaitItem()
            }
            assertThat(state.day).isNull()
            assertThat(state.entry).isNull()
        }
}
