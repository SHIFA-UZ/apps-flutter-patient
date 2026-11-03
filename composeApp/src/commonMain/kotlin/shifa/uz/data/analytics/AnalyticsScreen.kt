package shifa.uz.data.analytics

import shifa.uz.data.model.Appointment

/** Every screen the app sends as `$screen` (docs/posthog/events.md §2). Sent by `TrackScreen` in each `XxxScreen`. */
enum class AnalyticsScreen(val id: String) {
    Splash("splash"),
    Login("login"),
    Register("register"),
    ForgotPassword("forgot_password"),
    Home("home"),
    Bookings("bookings"),
    ShifaAi("shifa_ai"),
    Documents("documents"),
    Messages("messages"),
    BookingsList("bookings_list"),
    Upcoming("upcoming"),
    MyTasks("my_tasks"),
    Task("task"),
    AppointmentDetails("appointment_details"),
    Reschedule("reschedule"),
    LiveAppointment("live_appointment"),
    VideoCall("video_call"),
    DoctorProfile("doctor_profile"),
    CreateBooking("create_booking"),
    Payment("payment"),
    DocumentViewer("document_viewer"),
    AiChat("ai_chat"),
    Conversation("conversation"),
    Notifications("notifications"),
    Profile("profile"),
    EditProfile("edit_profile"),
    ChangePassword("change_password"),
    DeleteAccount("delete_account"),
}

/** An enum constant as an analytics value: `NewPassword` → `new_password`, `MyDoctors` → `my_doctors`. */
fun Enum<*>.analyticsName(): String = name.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), "_").lowercase()

/** `paid` / `pay_in_clinic` / `pending` / `refund` / `none`: how the visit is paid, as the details card shows it. */
fun Appointment.paymentBadge(): String =
    when {
        refundRequired -> "refund"
        isPaid -> "paid"
        isPayInClinic -> "pay_in_clinic"
        isAwaitingPayment || paymentStatus.equals("PENDING", ignoreCase = true) -> "pending"
        else -> "none"
    }

/** `upcoming` / `past` / `cancelled` for the appointment details screen. */
fun appointmentState(
    appointment: Appointment,
    isUpcoming: Boolean,
): String =
    when {
        appointment.status == "CANCELLED" || appointment.isPaymentDead -> "cancelled"
        isUpcoming -> "upcoming"
        else -> "past"
    }

/** `pdf` / `image` / `other` from an extension or MIME type hint. */
fun fileType(hint: String?): String {
    val h = hint?.trim()?.lowercase().orEmpty()
    return when {
        h.endsWith("pdf") -> "pdf"
        h.startsWith("image/") || h in setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp") -> "image"
        else -> "other"
    }
}
