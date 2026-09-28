package shifa.uz.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
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

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun TaskCheckInSelectedPreview() =
    PreviewTheme {
        TaskCheckInContent(
            TaskCheckInUiState(
                pressure,
                slots =
                    listOf(
                        TaskCheckIn(1, "2026-09-28", "08:00:00"),
                        TaskCheckIn(2, "2026-09-28", "13:00:00"),
                    ),
                selected = 2,
                value = "128",
            ),
            now,
        )
    }

// Slots are PENDING but none is due yet.
@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun TaskCheckInWaitingPreview() =
    PreviewTheme { TaskCheckInContent(TaskCheckInUiState(pressure, slots = emptyList(), waiting = true), now) }
