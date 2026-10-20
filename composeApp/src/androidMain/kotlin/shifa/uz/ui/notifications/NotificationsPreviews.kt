package shifa.uz.ui.notifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import shifa.uz.data.model.AppNotification
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.RemoteCareTask
import shifa.uz.data.model.TaskProgress
import shifa.uz.ui.theme.ShifaColors
import shifa.uz.ui.widgets.Gallery
import shifa.uz.ui.widgets.PreviewTheme

private val now = Instant.parse("2026-09-28T09:00:00Z")

private val reminder = AppNotification(1, "", "", "APPOINTMENT_REMINDER", "2026-09-28T08:58:00Z")
private val document = AppNotification(2, "", "", "NEW_DOCUMENT", "2026-09-28T07:30:00Z")
private val task = AppNotification(3, "", "", "TASK_ASSIGNED", "2026-09-28T06:00:00Z")

private val inbox =
    listOf(
        reminder,
        document,
        task,
        AppNotification(4, "", "", "APPOINTMENT_CANCELLED", "2026-09-27T12:00:00Z"),
        AppNotification(5, "", "", "CONSULTATION_PAYMENT_REMINDER", "2026-09-27T08:15:00Z"),
        AppNotification(6, "", "", "TASK_COMPLETED", "2026-09-20T10:00:00Z", readAt = "2026-09-20T11:00:00Z"),
        AppNotification(7, "", "", "SIGNATURE_REQUESTED", "2026-09-19T15:40:00Z", doctorName = "Dr. Aziz Karimov"),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun NotificationsInboxPreview() =
    PreviewTheme { NotificationsContent(NotificationsUiState(inbox), NotificationFilter.All, now) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun NotificationsEmptyFilterPreview() =
    PreviewTheme { NotificationsContent(NotificationsUiState(listOf(reminder)), NotificationFilter.Documents, now) }

@Preview
@Composable
internal fun NotificationAppointmentTrayPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        NotificationTray(
            reminder,
            NotificationLink.ToAppointment(
                Appointment(
                    doctorName = "Aziz Karimov",
                    doctorProfession = "Dentist",
                    doctorClinic = "Sog'lom Hayot Klinikasi",
                    startAt = "2026-09-28T10:00:00Z",
                ),
            ),
            now,
            null,
        )
    }

@Preview
@Composable
internal fun NotificationDocumentTrayPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        NotificationTray(
            document,
            NotificationLink.ToDocument(
                PatientDocument(
                    "1",
                    "Blood test results",
                    date = "2026-09-28",
                    fileUrl = "https://f/1",
                    creatorLabel = "Doctor",
                    doctorName = "Dr. Aziza Karimova",
                    category = "BLOOD_TEST",
                ),
            ),
            now,
            null,
        )
    }

@Preview
@Composable
internal fun NotificationTaskTrayPreview() =
    Gallery(background = ShifaColors.Bg.Canvas) {
        NotificationTray(
            task,
            NotificationLink.ToTask(
                RemoteCareTask(
                    id = "1",
                    taskName = "Blood pressure",
                    description = "Morning and evening",
                    category = "VITAL",
                    startDate = "2026-09-28",
                    progress = TaskProgress(totalCheckIns = 14, pendingCheckIns = 14),
                ),
            ),
            now,
            null,
        )
    }
