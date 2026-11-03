package shifa.uz.data.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import shifa.uz.data.model.ApiException

/** docs/posthog/development.md §3: the catalog's names and keys, so the usual privacy slips fail before review. */
class AnalyticsEventTest {
    // Every event class, as one instance. Add a line per new event in AnalyticsEvent.kt.
    private val catalog =
        listOf(
            AnalyticsEvent.AnalyticsOptedOut,
            AnalyticsEvent.AnalyticsOptedIn,
            AnalyticsEvent.SessionRestored("signed_in"),
            AnalyticsEvent.LoginSucceeded("email"),
            AnalyticsEvent.LoginFailed("network"),
            AnalyticsEvent.LoginLockedOut,
            AnalyticsEvent.SignupStarted,
            AnalyticsEvent.SignupStepCompleted("info", true, true, true, 1),
            AnalyticsEvent.SignupIdentifierConflict("patient_exists"),
            AnalyticsEvent.SignupCodeSent("sms", false),
            AnalyticsEvent.SignupSmsUnavailable("limit"),
            AnalyticsEvent.SignupFallbackEmail,
            AnalyticsEvent.SignupCompleted("sms", false, false, "2026-11-03T00:00:00Z"),
            AnalyticsEvent.SignupFailed("otp", "server"),
            AnalyticsEvent.SignupAbandoned("info"),
            AnalyticsEvent.PasswordResetRequested("email", false),
            AnalyticsEvent.PasswordResetCompleted,
            AnalyticsEvent.PasswordResetFailed("code", "server"),
            AnalyticsEvent.LoggedOut("profile"),
            AnalyticsEvent.HomeRefreshed,
            AnalyticsEvent.HomeSectionTapped("live", "view"),
            AnalyticsEvent.DoctorSearchPerformed(3, 1, "all"),
            AnalyticsEvent.DoctorFilterSelected("all"),
            AnalyticsEvent.BookingsListOpened("past"),
            AnalyticsEvent.DoctorProfileViewed(true, true, 2),
            AnalyticsEvent.DoctorProfileTabViewed("about"),
            AnalyticsEvent.DoctorServiceExpanded,
            AnalyticsEvent.DoctorContactTapped("phone"),
            AnalyticsEvent.DoctorCertificateOpened,
            AnalyticsEvent.BookingStarted,
            AnalyticsEvent.BookingLocationSelected(2),
            AnalyticsEvent.BookingServiceSelected("pay_now", true, true, 3),
            AnalyticsEvent.BookingVisitTypeChanged(true),
            AnalyticsEvent.BookingSlotsLoaded(null, true, "today"),
            AnalyticsEvent.BookingDatePicked(3),
            AnalyticsEvent.BookingSlotSelected(40, 10),
            AnalyticsEvent.BookingAttachmentAdded(1),
            AnalyticsEvent.BookingAttachmentFailed("network"),
            AnalyticsEvent.BookingCompleted(true, 1, 10, "free", false, 0, "2026-11-03T00:00:00Z"),
            AnalyticsEvent.BookingFailed("slot_taken"),
            AnalyticsEvent.BookingAbandoned("slot", false),
            AnalyticsEvent.AppointmentDetailsViewed("upcoming", true, "paid", true, true),
            AnalyticsEvent.AppointmentDetailsToggled(true, "details"),
            AnalyticsEvent.AppointmentCancelStarted(5),
            AnalyticsEvent.AppointmentCancelled(5, true, true),
            AnalyticsEvent.AppointmentCancelFailed("locked_24h"),
            AnalyticsEvent.AppointmentContactTapped("phone"),
            AnalyticsEvent.RescheduleStarted(true, 5),
            AnalyticsEvent.AppointmentRescheduled(1, true),
            AnalyticsEvent.RescheduleFailed("server"),
            AnalyticsEvent.ReviewStarted(2),
            AnalyticsEvent.ReviewSubmitted(5, true, 2),
            AnalyticsEvent.ReviewFailed("server"),
            AnalyticsEvent.ReviewAbandoned(true),
            AnalyticsEvent.PaymentMethodsLoaded(1, listOf("click"), "<50k", "UZS"),
            AnalyticsEvent.PaymentMethodsFailed("server"),
            AnalyticsEvent.PaymentProviderSelected("click", 2),
            AnalyticsEvent.PaymentMethodsClosed(true),
            AnalyticsEvent.PaymentCheckoutOpened("click", 900, "<50k", "UZS", false),
            AnalyticsEvent.PaymentCheckoutFailed("click", "server"),
            AnalyticsEvent.PaymentCheckTapped("pending", 10),
            AnalyticsEvent.PaymentRedirectSeen("success", "click"),
            AnalyticsEvent.PaymentCompleted("click", 60, "poll", "<50k", "UZS", false, 1, 10, "2026-11-03T00:00:00Z"),
            AnalyticsEvent.PaymentFailed("expired", "click", 900),
            AnalyticsEvent.PaymentAbandoned(true),
            AnalyticsEvent.PaymentBookAgainTapped("expired"),
            AnalyticsEvent.PaymentStillPendingShown,
            AnalyticsEvent.LiveAppointmentViewed(-5, true, true),
            AnalyticsEvent.VideoCallOpened(-5, true),
            AnalyticsEvent.VideoPermissionsResult(true),
            AnalyticsEvent.VideoJoinTapped,
            AnalyticsEvent.VideoJoined(100, "2026-11-03T00:00:00Z"),
            AnalyticsEvent.VideoJoinFailed("daily_failed"),
            AnalyticsEvent.VideoDoctorJoined(30),
            AnalyticsEvent.VideoCallMinimized,
            AnalyticsEvent.VideoCallExpanded,
            AnalyticsEvent.VideoControlToggled("camera", false),
            AnalyticsEvent.VideoCallEnded("patient", 600, true, false),
            AnalyticsEvent.SignatureStarted,
            AnalyticsEvent.SignatureSubmitted,
            AnalyticsEvent.SignatureFailed("empty"),
            AnalyticsEvent.SignatureAbandoned,
            AnalyticsEvent.TasksViewed("open", 1, 0),
            AnalyticsEvent.TasksRefreshed,
            AnalyticsEvent.TaskOpened("VITAL", "NUMERIC", "ACTIVE", 50),
            AnalyticsEvent.TaskRefreshed,
            AnalyticsEvent.TaskMonthChanged("prev", -1),
            AnalyticsEvent.CheckinDayOpened("due", 0),
            AnalyticsEvent.CheckinSubmitted("NUMERIC", "VITAL", false, 5, 9, "2026-11-03T00:00:00Z"),
            AnalyticsEvent.CheckinValidationFailed("value", "required"),
            AnalyticsEvent.CheckinFailed("server"),
            AnalyticsEvent.CheckinNothingDue,
            AnalyticsEvent.CheckinAbandoned("TEXT"),
            AnalyticsEvent.DocumentsRefreshed,
            AnalyticsEvent.DocumentsSearched(3, 1),
            AnalyticsEvent.DocumentsFiltered("MRI", 1),
            AnalyticsEvent.DocumentUploadStarted("camera"),
            AnalyticsEvent.DocumentUploadChanged,
            AnalyticsEvent.DocumentUploaded("camera", "pdf", "none", "<500", false, "2026-11-03T00:00:00Z"),
            AnalyticsEvent.DocumentUploadFailed("server"),
            AnalyticsEvent.DocumentUploadCancelled,
            AnalyticsEvent.DocumentViewed("pdf", "doctor", true),
            AnalyticsEvent.DocumentViewFailed("server"),
            AnalyticsEvent.DocumentExpanded(true),
            AnalyticsEvent.DocumentVisitOpened,
            AnalyticsEvent.DocumentDownloaded("pdf"),
            AnalyticsEvent.DocumentDownloadFailed,
            AnalyticsEvent.DocumentDeleted("swipe"),
            AnalyticsEvent.DocumentDeleteFailed("viewer", "server"),
            AnalyticsEvent.ChatsRefreshed,
            AnalyticsEvent.ChatDoctorSearched(2, 1),
            AnalyticsEvent.ChatOpened("list", false),
            AnalyticsEvent.ChatMessageSent("voice", false, null, "<10", null, "2026-11-03T00:00:00Z"),
            AnalyticsEvent.ChatMessageFailed("image", "upload", "network"),
            AnalyticsEvent.ChatRecordingStarted,
            AnalyticsEvent.ChatRecordingFailed,
            AnalyticsEvent.ChatAttachmentOpened("image"),
            AnalyticsEvent.ChatVoicePlayed(null),
            AnalyticsEvent.AiChatStarted(true, 0),
            AnalyticsEvent.AiMessageSent(1, "typed", "<50", "2026-11-03T00:00:00Z"),
            AnalyticsEvent.AiReplyReceived(100, 900, "<300", false),
            AnalyticsEvent.AiReplyFailed("rate_limit", false),
            AnalyticsEvent.AiRateLimited(3),
            AnalyticsEvent.AiReplyAbandoned(500),
            AnalyticsEvent.AiReplyAction("copy", 1),
            AnalyticsEvent.AiVoiceRecorded(4),
            AnalyticsEvent.AiVoiceTranscribed(800, "<50"),
            AnalyticsEvent.AiVoiceFailed("permission_denied"),
            AnalyticsEvent.AiChatDeleted(4),
            AnalyticsEvent.AiSearched(2, 1),
            AnalyticsEvent.AiGatedSeen("PRO"),
            AnalyticsEvent.PushPermissionResult(true),
            AnalyticsEvent.PushTokenRegistered,
            AnalyticsEvent.PushReceived("TASK_REMINDER"),
            AnalyticsEvent.PushOpened("TASK_REMINDER", "task"),
            AnalyticsEvent.NotificationsViewed(10, 2),
            AnalyticsEvent.NotificationsPageLoaded(2),
            AnalyticsEvent.NotificationsRefreshed,
            AnalyticsEvent.NotificationFilterSelected("tasks"),
            AnalyticsEvent.NotificationOpened("TASK_REMINDER", true, "<1"),
            AnalyticsEvent.NotificationActionTapped("TASK_REMINDER", "task"),
            AnalyticsEvent.NotificationsAllRead(2),
            AnalyticsEvent.ProfileRefreshed,
            AnalyticsEvent.ProfileRowTapped("privacy"),
            AnalyticsEvent.LanguageChanged("en", "uz", "profile"),
            AnalyticsEvent.ProfileUpdated(listOf("phone")),
            AnalyticsEvent.ProfileUpdateFailed("validation"),
            AnalyticsEvent.ProfilePhotoUpdated("profile"),
            AnalyticsEvent.ProfilePhotoFailed("profile", "network"),
            AnalyticsEvent.PasswordChanged,
            AnalyticsEvent.PasswordChangeFailed("wrong_current"),
            AnalyticsEvent.AccountDeletionStarted,
            AnalyticsEvent.AccountDeletionCodeResent,
            AnalyticsEvent.AccountDeletionFailed("confirm", "invalid_code"),
            AnalyticsEvent.AccountDeleted,
            AnalyticsEvent.PrivacyPolicyOpened,
            AnalyticsEvent.ComingSoonTapped("map"),
            AnalyticsEvent.RetryTapped("doctor_profile"),
            AnalyticsEvent.ApiError("patients/me/appointments/{id}", "GET", 404, "NotFound", "NOT_FOUND"),
            AnalyticsEvent.ApiSlow("public/doctors", "GET", 6_000),
            AnalyticsEvent.AiStreamError("connect", 500),
            AnalyticsEvent.WebviewError("click", -2),
        )

    // privacy.md §3: identity, free text, exact amounts and record ids never travel.
    private val blockedKeys =
        setOf(
            "name", "first_name", "last_name", "phone", "email", "text", "comment", "message", "query", "notes",
            "value", "answer", "title", "url", "amount", "address", "birth_date",
        )

    @Test
    fun `event names are snake_case`() {
        catalog.forEach { assertThat(it.name).matches("[a-z][a-z0-9]*(_[a-z0-9]+)*") }
    }

    @Test
    fun `no property key is personal data or a record id`() {
        catalog.forEach { event ->
            val keys = event.properties.keys + event.userProperties.keys + event.userPropertiesSetOnce.keys
            keys.forEach { key ->
                assertThat(key).matches("[a-z][a-z0-9]*(_[a-z0-9]+)*")
                assertThat(key).isNotIn(blockedKeys)
                assertThat(key).doesNotMatch(".*_id$")
            }
        }
    }

    @Test
    fun `the size, duration, text and age buckets are coarse`() {
        assertThat(sizeKbBucket(10 * 1024)).isEqualTo("<500")
        assertThat(sizeKbBucket(3_000 * 1024)).isEqualTo("2000-10000")
        assertThat(durationSBucket(45)).isEqualTo("30-60")
        assertThat(charsBucket(120)).isEqualTo("50-200")
        assertThat(replyCharsBucket(5_000)).isEqualTo(">1000")
        val now = kotlinx.datetime.Instant.parse("2026-11-03T12:00:00Z")
        assertThat(ageHoursBucket(kotlinx.datetime.Instant.parse("2026-11-03T11:30:00Z"), now)).isEqualTo("<1")
        assertThat(ageHoursBucket(kotlinx.datetime.Instant.parse("2026-10-20T12:00:00Z"), now)).isEqualTo(">7d")
    }

    @Test
    fun `the money and time helpers bucket and round`() {
        assertThat(amountBucket(4_999_900, "UZS")).isEqualTo("<50k")
        assertThat(amountBucket(10_000_000, "uzs")).isEqualTo("50k-150k")
        assertThat(amountBucket(50_000_000, "UZS")).isEqualTo(">500k")
        assertThat(amountBucket(1_000, "USD")).isEqualTo("foreign")
        assertThat(amountBucket(null, "UZS")).isNull()
        assertThat(ApiExceptionSamples.conflict.analyticsReason()).isEqualTo("conflict")
        assertThat(IllegalStateException().analyticsReason()).isEqualTo("server")
    }

    @Test
    fun `null property values are left out instead of sent as null`() {
        assertThat(listOf("a" to 1, "b" to null, "c" to "x").clean()).containsExactly("a", 1, "c", "x")
    }
}

/** For ViewModel tests: the events a ViewModel sent, in order. */
class RecordingTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()

    override fun track(event: AnalyticsEvent) {
        events += event
    }
}

private object ApiExceptionSamples {
    val conflict = ApiException.Conflict("taken")
}
