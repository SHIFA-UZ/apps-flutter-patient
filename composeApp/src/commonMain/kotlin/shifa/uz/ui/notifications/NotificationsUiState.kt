package shifa.uz.ui.notifications

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.*
import shifa.uz.data.model.AppNotification
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.PatientDocument
import shifa.uz.data.model.RemoteCareTask

/** 13 §4.1, matched on the uppercased type. Figma's order, then Payments (not in Figma). */
enum class NotificationFilter(
    val title: StringResource,
    val matches: (String) -> Boolean,
) {
    All(Res.string.notificationFilterAll, { true }),
    Appointments(Res.string.notificationFilterAppointments, { "APPOINTMENT" in it }),
    Tasks(Res.string.notificationFilterTasks, { "TASK" in it }),
    Documents(Res.string.notificationFilterDocuments, { "DOCUMENT" in it || "SIGNATURE" in it }),
    Payments(Res.string.notificationFilterPayments, { "PAYMENT" in it || "INSTALLMENT" in it }),
}

/** Where the details tray's button goes; the ones with an item also show its card. */
sealed interface NotificationLink {
    data class ToAppointment(
        val appointment: Appointment,
    ) : NotificationLink

    data class ToDocument(
        val document: PatientDocument,
    ) : NotificationLink

    data class ToTask(
        val task: RemoteCareTask,
    ) : NotificationLink

    /** SIGNATURE_REQUESTED: the visit's Live Appointment page, where its Request to sign is. */
    data class ToSigning(
        val appointment: Appointment,
    ) : NotificationLink

    data object ToBookings : NotificationLink

    data object ToDocuments : NotificationLink

    data object ToTasks : NotificationLink

    data class ToChat(
        val chatId: String,
    ) : NotificationLink

    data object ToMessages : NotificationLink
}

/** [notifications] is null until the first load; a failed reload keeps the previous list. */
@Stable
data class NotificationsUiState(
    val notifications: List<AppNotification>? = null,
    val isLoading: Boolean = false,
    val error: StringResource? = null,
    val notice: StringResource? = null,
    val selected: AppNotification? = null,
    val link: NotificationLink? = null,
) {
    /** Newest first (13 §4.1). */
    fun shownIn(filter: NotificationFilter) =
        notifications?.filter { filter.matches(it.kind) }?.sortedByDescending { it.createdAt }
}

/** 13 §6.3: the localized title and message per type. SIGNATURE_REQUESTED and DOCUMENT_ACCESS_REQUEST fill theirs in. */
internal val NOTIFICATION_TEXTS: Map<String, Pair<StringResource, StringResource>> =
    listOf(
        listOf("APPOINTMENT_CANCELLED", "APPOINTMENT_CANCELLED_BY_PATIENT") to
            (Res.string.appointmentCancelledTitle to Res.string.notificationMessageCancelled),
        listOf("APPOINTMENT_CHANGED", "APPOINTMENT_RESCHEDULED_BY_PATIENT") to
            (Res.string.appointmentChangedTitle to Res.string.notificationMessageChanged),
        listOf("APPOINTMENT_REMINDER", "APPOINTMENT_UPCOMING", "APPOINTMENT_SOON", "APPOINTMENT_CLOSE") to
            (Res.string.appointmentReminderTitle to Res.string.notificationMessageReminder),
        listOf("APPOINTMENT_SCHEDULED", "NEW_APPOINTMENT", "APPOINTMENT_ASSIGNED", "APPOINTMENT_BOOKED_BY_PATIENT") to
            (Res.string.newAppointmentScheduledTitle to Res.string.notificationMessageScheduled),
        listOf("CONSULTATION_PAYMENT_REMINDER") to
            (Res.string.consultationPaymentReminderTitle to Res.string.consultationPaymentReminderMessage),
        listOf("CONSULTATION_PAYMENT_DUE_24H") to
            (Res.string.consultationPaymentReminderTitle to Res.string.consultationPaymentDue24hMessage),
        listOf("CONSULTATION_PAYMENT_DUE_6H") to
            (Res.string.consultationPaymentReminderTitle to Res.string.consultationPaymentDue6hMessage),
        listOf("CONSULTATION_PAYMENT_DUE_1H") to
            (Res.string.consultationPaymentReminderTitle to Res.string.consultationPaymentDue1hMessage),
        listOf("DOCUMENT_ACCESS_APPROVED") to
            (Res.string.documentAccessApprovedTitle to Res.string.documentAccessApprovedMessage),
        listOf("DOCUMENT_ACCESS_REJECTED") to
            (Res.string.documentAccessRejectedTitle to Res.string.documentAccessRejectedMessage),
        listOf("INSTALLMENT_DUE_SOON") to
            (Res.string.notificationInstallmentDueSoonTitle to Res.string.notificationInstallmentDueSoonMessage),
        listOf("INSTALLMENT_DUE_TODAY") to
            (Res.string.notificationInstallmentDueTodayTitle to Res.string.notificationInstallmentDueTodayMessage),
        listOf("INSTALLMENT_OVERDUE") to
            (Res.string.notificationInstallmentOverdueTitle to Res.string.notificationInstallmentOverdueMessage),
        listOf("INSTALLMENT_SCHEDULE_CREATED") to
            (
                Res.string.notificationInstallmentScheduleCreatedTitle to
                    Res.string.notificationInstallmentScheduleCreatedMessage
            ),
        listOf("TREATMENT_PLAN_PAYMENT_REMINDER") to
            (Res.string.treatmentPlanPaymentReminderTitle to Res.string.treatmentPlanPaymentReminderMessage),
        listOf("TREATMENT_PLAN_UPDATED") to
            (Res.string.treatmentPlanUpdatedTitle to Res.string.treatmentPlanUpdatedMessage),
        listOf(
            "PROPHYLAXIS_REMINDER",
        ) to (Res.string.prophylaxisReminderTitle to Res.string.prophylaxisReminderMessage),
        listOf("AI_VISIT_SUMMARY_READY", "AI_SCRIBE_READY") to
            (Res.string.visitSummaryReadyTitle to Res.string.visitSummaryReadyMessage),
        listOf("CHAT_MESSAGE", "NEW_MESSAGE") to (Res.string.chatNewMessageTitle to Res.string.chatNewMessageMessage),
        listOf("TASK_ASSIGNED") to (Res.string.taskAssignedTitle to Res.string.taskAssignedMessage),
        listOf("TASK_REMINDER") to (Res.string.taskReminderTitle to Res.string.taskReminderMessage),
        listOf("TASK_CANCELLED") to (Res.string.taskCancelledTitle to Res.string.taskCancelledMessage),
        listOf("TASK_COMPLETED") to (Res.string.taskCompletedTitle to Res.string.taskCompletedMessage),
        listOf("NEW_DOCUMENT", "DOCUMENT") to (Res.string.newDocumentTitle to Res.string.newDocumentMessage),
    ).flatMap { (types, text) -> types.map { it to text } }.toMap()

private val ACCESS_REQUEST = Regex("""^(.+) requested access to "(.+)" for (.+)\.$""")

/** 13 §6.3: requester, file and patient from the backend's English text, or null when it doesn't match. */
internal fun accessRequestParts(message: String) = ACCESS_REQUEST.matchEntire(message.trim())?.destructured?.toList()

/** 13 §4.2, first match wins. Only shown with a [NotificationLink]. */
internal fun actionLabel(kind: String) =
    when {
        kind == "PROPHYLAXIS_REMINDER" -> Res.string.notificationActionOpenBookings
        kind == "SIGNATURE_REQUESTED" -> Res.string.signNow
        kind.startsWith("AI_") -> Res.string.notificationActionOpenDocuments
        "APPOINTMENT" in kind -> Res.string.notificationActionOpenAppointment
        "DOCUMENT" in kind -> Res.string.notificationActionViewDocument
        "TASK" in kind -> Res.string.notificationActionOpenTask
        "MESSAGE" in kind -> Res.string.notificationActionOpenChat
        else -> Res.string.notificationActionOpenDetails
    }
