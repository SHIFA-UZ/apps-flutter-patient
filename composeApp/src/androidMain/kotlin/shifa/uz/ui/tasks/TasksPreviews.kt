package shifa.uz.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.checkInSubmittedSuccessfully
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskCheckIn
import shifa.uz.data.model.TaskProgress
import shifa.uz.ui.widgets.PreviewTheme

private val now = Instant.parse("2026-09-28T09:00:00Z")

private val pressure =
    RemoteCareTask(
        id = "1",
        taskName = "Blood pressure",
        description = "Morning and evening",
        category = "VITAL",
        startDate = "2026-09-20",
        inputType = "NUMERIC",
        inputLabel = "systolic",
        notesLabel = "How do you feel?",
        progress = TaskProgress(totalCheckIns = 14, completedCheckIns = 9, pendingCheckIns = 5),
    )

private val tasks =
    listOf(
        pressure,
        RemoteCareTask(
            id = "2",
            taskName = "Running",
            description = "30 minutes, easy pace",
            category = "EXERCISE",
            startDate = "2026-09-29",
            progress = TaskProgress(totalCheckIns = 10, pendingCheckIns = 10),
        ),
        RemoteCareTask(
            id = "3",
            taskName = "Vitamin D",
            category = "MEDICATION",
            status = "COMPLETED",
            startDate = "2026-09-01",
            progress = TaskProgress(totalCheckIns = 7, completedCheckIns = 7),
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun TasksOpenPreview() =
    PreviewTheme {
        TasksContent(TasksUiState(tasks, notice = Res.string.checkInSubmittedSuccessfully), TasksTab.Open, now)
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun TasksCompletedPreview() = PreviewTheme { TasksContent(TasksUiState(tasks), TasksTab.Completed, now) }

// September 2026 every other day: submitted up to the 26th, the 22nd missed, today (28th) due, then PENDING.
private val history =
    (14..30 step 2).map { day ->
        val status =
            when {
                day == 22 -> "MISSED"
                day < 28 -> "COMPLETED"
                else -> "PENDING"
            }
        TaskCheckIn(day.toLong(), "2026-09-$day", "13:00:00", status, numericValue = 120.0 + day, notes = "Felt fine")
    }

private val calendar = TaskCheckInUiState(pressure, LocalDate(2026, 9, 1), history)

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun TaskCalendarPreview() = PreviewTheme { TaskCheckInContent(calendar, now) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun TaskSubmitTrayPreview() =
    PreviewTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            DayTray(calendar.copy(day = LocalDate(2026, 9, 28), entry = history[7], entryDue = true, value = "128"))
        }
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun TaskSubmittedTrayPreview() =
    PreviewTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            DayTray(calendar.copy(day = LocalDate(2026, 9, 24)))
        }
    }
