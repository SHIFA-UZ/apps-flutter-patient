package shifa.uz.ui.debug

import androidx.compose.runtime.Stable

/** A push like the backend's, of [type], linked to the entity in [link] when there is one (13 §6.2). */
data class FakePush(
    val label: String,
    val type: String,
    val message: String,
    val link: Pair<String, String?>? = null,
) {
    // No notificationId: the tap would mark a server notification read (N3).
    val data get() =
        buildMap {
            put("type", type)
            put("title", label)
            put("message", message)
            link?.second?.let { put(link.first, it) }
        }
}

/** The ids are the current backend's first ones (the demo's when demo data is on), null until loaded or if none. */
@Stable
data class DebugUiState(
    val demo: Boolean = false,
    val development: Boolean = false, // the development backend instead of production
    val appointmentId: String? = null,
    val taskId: String? = null,
    val documentId: String? = null,
    val chatId: String? = null,
    val notice: String? = null,
    val noticeIsError: Boolean = false,
) {
    val pushes get() =
        listOf(
            FakePush(
                "Appointment confirmed",
                "APPOINTMENT_CONFIRMED",
                "Your visit is confirmed.",
                "appointmentId" to appointmentId,
            ),
            FakePush(
                "Appointment reminder",
                "APPOINTMENT_REMINDER",
                "Your visit starts soon.",
                "appointmentId" to appointmentId,
            ),
            FakePush(
                "Payment due in 24 h",
                "CONSULTATION_PAYMENT_DUE_24H",
                "Pay to keep your slot.",
                "appointmentId" to appointmentId,
            ),
            FakePush("Task check-in due", "TASK_REMINDER", "Time for your check-in.", "taskId" to taskId),
            FakePush("New document", "NEW_DOCUMENT", "A doctor shared a document.", "documentId" to documentId),
            FakePush("New chat message", "NEW_MESSAGE", "You have a new message.", "chatId" to chatId),
            FakePush(
                "Visit summary ready",
                "AI_VISIT_SUMMARY_READY",
                "Your after-visit summary is now available.",
                "appointmentId" to appointmentId,
            ),
            FakePush("Treatment plan updated", "TREATMENT_PLAN_UPDATED", "Your treatment plan changed."),
        )
}
