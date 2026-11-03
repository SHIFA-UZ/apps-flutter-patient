package shifa.uz.data.analytics

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import shifa.uz.data.model.ApiException

/**
 * The event catalog in code: one class per row of docs/posthog/events.md, and nothing else is ever sent. Names
 * are `snake_case`, values are our own enums, counts and buckets; no identity, health data, record ids or free text
 * (privacy.md §3). `AnalyticsEventTest` checks the names and property keys.
 */
sealed class AnalyticsEvent(
    val name: String,
    vararg props: Pair<String, Any?>,
    /** Person properties set with the event (personalisation.md §2): `has_booked = true`, … */
    val userProperties: Map<String, Any> = emptyMap(),
    /** Person properties set only the first time: `first_booking_at`, … */
    val userPropertiesSetOnce: Map<String, Any> = emptyMap(),
) {
    val properties: Map<String, Any> = props.toList().clean()

    /** For the Debug menu's list. (Not toString: data objects generate their own.) */
    val label: String get() = if (properties.isEmpty()) name else "$name $properties"

    // ---- events.md §3: launch and auth ----

    /** `signed_in` / `no_token` / `expired` / `offline` / `reinstall_wipe`. */
    data class SessionRestored(val result: String) : AnalyticsEvent("session_restored", "result" to result)

    /** `email` / `phone`. */
    data class LoginSucceeded(val identifierType: String) :
        AnalyticsEvent("login_succeeded", "identifier_type" to identifierType)

    /** `empty_fields` / `invalid_credentials` / `locked_out` / `network` / `server`. */
    data class LoginFailed(val reason: String) : AnalyticsEvent("login_failed", "reason" to reason)

    data object LoginLockedOut : AnalyticsEvent("login_locked_out")

    data object SignupStarted : AnalyticsEvent("signup_started")

    /** [step]: `account` / `info` / `photo`. */
    data class SignupStepCompleted(
        val step: String,
        val hasEmail: Boolean,
        val hasPhone: Boolean,
        val hasPhoto: Boolean? = null,
        val filledOptional: Int? = null,
    ) : AnalyticsEvent(
            "signup_step_completed",
            "step" to step,
            "has_email" to hasEmail,
            "has_phone" to hasPhone,
            "has_photo" to hasPhoto,
            "filled_optional" to filledOptional,
        )

    /** `patient_exists` / `doctor_exists`. */
    data class SignupIdentifierConflict(val kind: String) :
        AnalyticsEvent("signup_identifier_conflict", "kind" to kind)

    data class SignupCodeSent(val channel: String, val isResend: Boolean) :
        AnalyticsEvent("signup_code_sent", "channel" to channel, "is_resend" to isResend)

    /** `limit` / `channel_email` / an API reason. */
    data class SignupSmsUnavailable(val cause: String) : AnalyticsEvent("signup_sms_unavailable", "cause" to cause)

    data object SignupFallbackEmail : AnalyticsEvent("signup_fallback_email")

    data class SignupCompleted(val channel: String, val isDoctorLink: Boolean, val hasPhoto: Boolean, val at: String) :
        AnalyticsEvent(
            "signup_completed",
            "channel" to channel,
            "is_doctor_link" to isDoctorLink,
            "has_photo" to hasPhoto,
            userPropertiesSetOnce =
                mapOf("signup_method" to if (isDoctorLink) "doctor_link" else channel, "signed_up_at" to at),
        )

    data class SignupFailed(val step: String, val reason: String) :
        AnalyticsEvent("signup_failed", "step" to step, "reason" to reason)

    data class SignupAbandoned(val step: String) : AnalyticsEvent("signup_abandoned", "step" to step)

    data class PasswordResetRequested(val channel: String, val isResend: Boolean) :
        AnalyticsEvent("password_reset_requested", "channel" to channel, "is_resend" to isResend)

    data object PasswordResetCompleted : AnalyticsEvent("password_reset_completed")

    data class PasswordResetFailed(val step: String, val reason: String) :
        AnalyticsEvent("password_reset_failed", "step" to step, "reason" to reason)

    /** `profile` / `account_deleted` / `backend_switch`. */
    data class LoggedOut(val source: String) : AnalyticsEvent("logged_out", "source" to source)

    // ---- events.md §4: home, doctors and booking ----

    data object HomeRefreshed : AnalyticsEvent("home_refreshed")

    /** [section]: `live` / `upcoming` / `tasks` / `my_doctors`; [target]: `card` / `arrow` / `view`. */
    data class HomeSectionTapped(val section: String, val target: String) :
        AnalyticsEvent("home_section_tapped", "section" to section, "target" to target)

    data class DoctorSearchPerformed(val queryLength: Int, val resultsCount: Int, val chip: String) :
        AnalyticsEvent(
            "doctor_search_performed",
            "query_length" to queryLength,
            "results_count" to resultsCount,
            "chip" to chip,
        )

    data class DoctorFilterSelected(val chip: String) : AnalyticsEvent("doctor_filter_selected", "chip" to chip)

    /** `recommended` / `my_doctors` / `past`. */
    data class BookingsListOpened(val list: String) : AnalyticsEvent("bookings_list_opened", "list" to list)

    data class DoctorProfileViewed(val hasReviews: Boolean, val supportsOnline: Boolean, val servicesCount: Int) :
        AnalyticsEvent(
            "doctor_profile_viewed",
            "has_reviews" to hasReviews,
            "supports_online" to supportsOnline,
            "services_count" to servicesCount,
        )

    data class DoctorProfileTabViewed(val tab: String) : AnalyticsEvent("doctor_profile_tab_viewed", "tab" to tab)

    data object DoctorServiceExpanded : AnalyticsEvent("doctor_service_expanded")

    /** `phone` / `email` / `telegram` / `instagram` / `other`. */
    data class DoctorContactTapped(val channel: String) : AnalyticsEvent("doctor_contact_tapped", "channel" to channel)

    data object DoctorCertificateOpened : AnalyticsEvent("doctor_certificate_opened")

    data object BookingStarted : AnalyticsEvent("booking_started")

    data class BookingLocationSelected(val locationsCount: Int) :
        AnalyticsEvent("booking_location_selected", "locations_count" to locationsCount)

    data class BookingServiceSelected(
        val paymentKind: String,
        val hasPrice: Boolean,
        val allowsVideo: Boolean,
        val servicesCount: Int,
    ) : AnalyticsEvent(
            "booking_service_selected",
            "payment_kind" to paymentKind,
            "has_price" to hasPrice,
            "allows_video" to allowsVideo,
            "services_count" to servicesCount,
        )

    data class BookingVisitTypeChanged(val isVideo: Boolean) :
        AnalyticsEvent("booking_visit_type_changed", "is_video" to isVideo)

    /** [daysToFirstSlot]: null is `none`; [scanStart]: `next_available` / `today` / `picked`. */
    data class BookingSlotsLoaded(val daysToFirstSlot: Int?, val isVideo: Boolean, val scanStart: String) :
        AnalyticsEvent(
            "booking_slots_loaded",
            "days_to_first_slot" to (daysToFirstSlot ?: "none"),
            "is_video" to isVideo,
            "scan_start" to scanStart,
        )

    data class BookingDatePicked(val daysAhead: Int) : AnalyticsEvent("booking_date_picked", "days_ahead" to daysAhead)

    data class BookingSlotSelected(val hoursAhead: Int, val hourOfDay: Int) :
        AnalyticsEvent("booking_slot_selected", "hours_ahead" to hoursAhead, "hour_of_day" to hourOfDay)

    data class BookingAttachmentAdded(val count: Int) : AnalyticsEvent("booking_attachment_added", "count" to count)

    data class BookingAttachmentFailed(val reason: String) :
        AnalyticsEvent("booking_attachment_failed", "reason" to reason)

    data class BookingCompleted(
        val isVideo: Boolean,
        val daysAhead: Int?,
        val hourOfDay: Int?,
        val paymentKind: String,
        val hasReason: Boolean,
        val attachments: Int,
        val at: String,
    ) : AnalyticsEvent(
            "booking_completed",
            "is_video" to isVideo,
            "days_ahead" to daysAhead,
            "hour_of_day" to hourOfDay,
            "payment_kind" to paymentKind,
            "has_reason" to hasReason,
            "attachments" to attachments,
            userProperties = bookedProperties(isVideo),
            userPropertiesSetOnce = mapOf("first_booking_at" to at),
        )

    /** `needs_service` / `needs_video_service` / `double_booking` / `slot_taken` / `locked_24h` / an API reason. */
    data class BookingFailed(val reason: String) : AnalyticsEvent("booking_failed", "reason" to reason)

    /** [stepReached]: `opened` / `service` / `slot` / `payment_methods` / `checkout`. */
    data class BookingAbandoned(val stepReached: String, val isReschedule: Boolean) :
        AnalyticsEvent("booking_abandoned", "step_reached" to stepReached, "is_reschedule" to isReschedule)

    data class AppointmentDetailsViewed(
        val state: String,
        val isVideo: Boolean,
        val paymentBadge: String,
        val canCancel: Boolean,
        val canReschedule: Boolean,
    ) : AnalyticsEvent(
            "appointment_details_viewed",
            "state" to state,
            "is_video" to isVideo,
            "payment_badge" to paymentBadge,
            "can_cancel" to canCancel,
            "can_reschedule" to canReschedule,
        )

    /** [screen]: `details` / `live_appointment`. */
    data class AppointmentDetailsToggled(val open: Boolean, val screen: String) :
        AnalyticsEvent("appointment_details_toggled", "open" to open, "screen" to screen)

    data class AppointmentCancelStarted(val hoursAhead: Int?) :
        AnalyticsEvent("appointment_cancel_started", "hours_ahead" to hoursAhead)

    data class AppointmentCancelled(val hoursAhead: Int?, val isVideo: Boolean, val wasPaid: Boolean) :
        AnalyticsEvent(
            "appointment_cancelled",
            "hours_ahead" to hoursAhead,
            "is_video" to isVideo,
            "was_paid" to wasPaid,
        )

    data class AppointmentCancelFailed(val reason: String) :
        AnalyticsEvent("appointment_cancel_failed", "reason" to reason)

    /** `phone` / `email`. */
    data class AppointmentContactTapped(val channel: String) :
        AnalyticsEvent("appointment_contact_tapped", "channel" to channel)

    data class RescheduleStarted(val isVideo: Boolean, val hoursAhead: Int?) :
        AnalyticsEvent("reschedule_started", "is_video" to isVideo, "hours_ahead" to hoursAhead)

    data class AppointmentRescheduled(val daysMoved: Int?, val isVideo: Boolean) :
        AnalyticsEvent("appointment_rescheduled", "days_moved" to daysMoved, "is_video" to isVideo)

    data class RescheduleFailed(val reason: String) : AnalyticsEvent("reschedule_failed", "reason" to reason)

    data class ReviewStarted(val daysAfterVisit: Int?) :
        AnalyticsEvent("review_started", "days_after_visit" to daysAfterVisit)

    data class ReviewSubmitted(val rating: Int, val hasComment: Boolean, val daysAfterVisit: Int?) :
        AnalyticsEvent(
            "review_submitted",
            "rating" to rating,
            "has_comment" to hasComment,
            "days_after_visit" to daysAfterVisit,
            userProperties = mapOf("has_reviewed" to true),
        )

    data class ReviewFailed(val reason: String) : AnalyticsEvent("review_failed", "reason" to reason)

    data class ReviewAbandoned(val hadRating: Boolean) : AnalyticsEvent("review_abandoned", "had_rating" to hadRating)

    // ---- events.md §5: payments ----

    data class PaymentMethodsLoaded(
        val count: Int,
        val providers: List<String>,
        val amountBucket: String?,
        val currency: String?,
    ) : AnalyticsEvent(
            "payment_methods_loaded",
            "count" to count,
            "providers" to providers,
            "amount_bucket" to amountBucket,
            "currency" to currency,
        )

    data class PaymentMethodsFailed(val reason: String) : AnalyticsEvent("payment_methods_failed", "reason" to reason)

    data class PaymentProviderSelected(val provider: String, val providersCount: Int) :
        AnalyticsEvent("payment_provider_selected", "provider" to provider, "providers_count" to providersCount)

    data class PaymentMethodsClosed(val hadProvider: Boolean) :
        AnalyticsEvent("payment_methods_closed", "had_provider" to hadProvider)

    data class PaymentCheckoutOpened(
        val provider: String?,
        val holdSeconds: Long?,
        val amountBucket: String?,
        val currency: String?,
        val isVideo: Boolean,
    ) : AnalyticsEvent(
            "payment_checkout_opened",
            "provider" to provider,
            "hold_seconds" to holdSeconds,
            "amount_bucket" to amountBucket,
            "currency" to currency,
            "is_video" to isVideo,
        )

    data class PaymentCheckoutFailed(val provider: String?, val reason: String) :
        AnalyticsEvent("payment_checkout_failed", "provider" to provider, "reason" to reason)

    /** [result]: `paid` / `pending` / `failed`. */
    data class PaymentCheckTapped(val result: String, val secondsOpen: Long) :
        AnalyticsEvent("payment_check_tapped", "result" to result, "seconds_open" to secondsOpen)

    /** [page]: `success` / `cancel`. */
    data class PaymentRedirectSeen(val page: String, val provider: String?) :
        AnalyticsEvent("payment_redirect_seen", "page" to page, "provider" to provider)

    /** [via]: `poll` / `check` / `redirect` / `close_check` / `immediate`. */
    data class PaymentCompleted(
        val provider: String?,
        val secondsToPay: Long,
        val via: String,
        val amountBucket: String?,
        val currency: String?,
        val isVideo: Boolean,
        val daysAhead: Int?,
        val hourOfDay: Int?,
        val at: String,
    ) : AnalyticsEvent(
            "payment_completed",
            "provider" to provider,
            "seconds_to_pay" to secondsToPay,
            "via" to via,
            "amount_bucket" to amountBucket,
            "currency" to currency,
            "payment_kind" to "pay_now",
            "is_video" to isVideo,
            "days_ahead" to daysAhead,
            "hour_of_day" to hourOfDay,
            userProperties =
                bookedProperties(
                    isVideo,
                ) + mapOf("has_paid" to true, "last_payment_provider" to (provider ?: "unknown")),
            userPropertiesSetOnce = mapOf("first_booking_at" to at, "first_payment_at" to at),
        )

    /** [reason]: `declined` / `expired` / `expired_paid_refund`. */
    data class PaymentFailed(val reason: String, val provider: String?, val secondsOpen: Long) :
        AnalyticsEvent(
            "payment_failed",
            "reason" to reason,
            "provider" to provider,
            "seconds_open" to secondsOpen,
        )

    data class PaymentAbandoned(val released: Boolean) : AnalyticsEvent("payment_abandoned", "released" to released)

    data class PaymentBookAgainTapped(val reason: String?) :
        AnalyticsEvent("payment_book_again_tapped", "reason" to reason)

    data object PaymentStillPendingShown : AnalyticsEvent("payment_still_pending_shown")

    // ---- events.md §6: the live visit, video call and signing ----

    data class LiveAppointmentViewed(val minutesToStart: Long?, val isVideo: Boolean, val signatureDue: Boolean) :
        AnalyticsEvent(
            "live_appointment_viewed",
            "minutes_to_start" to minutesToStart,
            "is_video" to isVideo,
            "signature_due" to signatureDue,
        )

    data class VideoCallOpened(val minutesToStart: Long?, val isVideo: Boolean) :
        AnalyticsEvent("video_call_opened", "minutes_to_start" to minutesToStart, "is_video" to isVideo)

    data class VideoPermissionsResult(val granted: Boolean) :
        AnalyticsEvent(
            "video_permissions_result",
            "granted" to granted,
            userProperties = mapOf("camera_mic_granted" to granted),
        )

    data object VideoJoinTapped : AnalyticsEvent("video_join_tapped")

    data class VideoJoined(val joinMs: Long?, val at: String) :
        AnalyticsEvent(
            "video_joined",
            "join_ms" to joinMs,
            userProperties = mapOf("has_video_visit" to true),
            userPropertiesSetOnce = mapOf("first_video_visit_at" to at),
        )

    /** `daily_failed` / an API reason. */
    data class VideoJoinFailed(val reason: String) : AnalyticsEvent("video_join_failed", "reason" to reason)

    data class VideoDoctorJoined(val waitedS: Long?) : AnalyticsEvent("video_doctor_joined", "waited_s" to waitedS)

    data object VideoCallMinimized : AnalyticsEvent("video_call_minimized")

    data object VideoCallExpanded : AnalyticsEvent("video_call_expanded")

    /** [control]: `camera` / `mic` / `flip`. */
    data class VideoControlToggled(val control: String, val isOn: Boolean?) :
        AnalyticsEvent("video_control_toggled", "control" to control, "is_on" to isOn)

    /** [endedBy]: `patient` / `doctor` / `error`. */
    data class VideoCallEnded(
        val endedBy: String,
        val durationS: Long?,
        val doctorJoined: Boolean,
        val wasMinimized: Boolean,
    ) :
        AnalyticsEvent(
                "video_call_ended",
                "ended_by" to endedBy,
                "duration_s" to durationS,
                "doctor_joined" to doctorJoined,
                "was_minimized" to wasMinimized,
            )

    data object SignatureStarted : AnalyticsEvent("signature_started")

    data object SignatureSubmitted : AnalyticsEvent("signature_submitted")

    /** `empty` / an API reason. */
    data class SignatureFailed(val reason: String) : AnalyticsEvent("signature_failed", "reason" to reason)

    data object SignatureAbandoned : AnalyticsEvent("signature_abandoned")

    // ---- events.md §7: tasks and check-ins ----

    /** [tab]: `open` / `completed`. */
    data class TasksViewed(val tab: String, val openCount: Int?, val completedCount: Int?) :
        AnalyticsEvent("tasks_viewed", "tab" to tab, "open_count" to openCount, "completed_count" to completedCount)

    data object TasksRefreshed : AnalyticsEvent("tasks_refreshed")

    data class TaskOpened(val category: String, val inputType: String, val status: String, val progressPct: Int?) :
        AnalyticsEvent(
            "task_opened",
            "category" to category,
            "input_type" to inputType,
            "status" to status,
            "progress_pct" to progressPct,
        )

    data object TaskRefreshed : AnalyticsEvent("task_refreshed")

    /** [direction]: `prev` / `next`. */
    data class TaskMonthChanged(val direction: String, val monthsFromNow: Int) :
        AnalyticsEvent("task_month_changed", "direction" to direction, "months_from_now" to monthsFromNow)

    /** [state]: `due` / `not_yet_due` / `submitted`. */
    data class CheckinDayOpened(val state: String, val daysFromToday: Int) :
        AnalyticsEvent("checkin_day_opened", "state" to state, "days_from_today" to daysFromToday)

    data class CheckinSubmitted(
        val inputType: String,
        val category: String,
        val hasNotes: Boolean,
        val minutesFromScheduled: Long?,
        val hourOfDay: Int,
        val at: String,
    ) : AnalyticsEvent(
            "checkin_submitted",
            "input_type" to inputType,
            "category" to category,
            "has_notes" to hasNotes,
            "minutes_from_scheduled" to minutesFromScheduled,
            "hour_of_day" to hourOfDay,
            userProperties = mapOf("has_checked_in" to true),
            userPropertiesSetOnce = mapOf("first_checkin_at" to at),
        )

    /** [field]: `value` / `notes`; [rule]: `required` / `not_a_number` / `yes_no`. */
    data class CheckinValidationFailed(val field: String, val rule: String) :
        AnalyticsEvent("checkin_validation_failed", "field" to field, "rule" to rule)

    data class CheckinFailed(val reason: String) : AnalyticsEvent("checkin_failed", "reason" to reason)

    data object CheckinNothingDue : AnalyticsEvent("checkin_nothing_due")

    data class CheckinAbandoned(val inputType: String) : AnalyticsEvent("checkin_abandoned", "input_type" to inputType)

    // ---- events.md §8: documents ----

    data object DocumentsRefreshed : AnalyticsEvent("documents_refreshed")

    data class DocumentsSearched(val queryLength: Int, val resultsCount: Int) :
        AnalyticsEvent("documents_searched", "query_length" to queryLength, "results_count" to resultsCount)

    data class DocumentsFiltered(val category: String, val resultsCount: Int) :
        AnalyticsEvent("documents_filtered", "category" to category, "results_count" to resultsCount)

    /** [source]: `camera` / `photo` / `file`. */
    data class DocumentUploadStarted(val source: String) : AnalyticsEvent("document_upload_started", "source" to source)

    data object DocumentUploadChanged : AnalyticsEvent("document_upload_changed")

    data class DocumentUploaded(
        val source: String?,
        val fileType: String,
        val category: String,
        val sizeKbBucket: String,
        val titleEdited: Boolean,
        val at: String,
    ) : AnalyticsEvent(
            "document_uploaded",
            "source" to source,
            "file_type" to fileType,
            "category" to category,
            "size_kb_bucket" to sizeKbBucket,
            "title_edited" to titleEdited,
            userProperties = mapOf("has_uploaded" to true),
            userPropertiesSetOnce = mapOf("first_upload_at" to at),
        )

    /** `title_missing` / an API reason. */
    data class DocumentUploadFailed(val reason: String) : AnalyticsEvent("document_upload_failed", "reason" to reason)

    data object DocumentUploadCancelled : AnalyticsEvent("document_upload_cancelled")

    /** [uploadedBy]: `self` / `doctor` / `unknown`. */
    data class DocumentViewed(val fileType: String, val uploadedBy: String, val hasVisit: Boolean) :
        AnalyticsEvent(
            "document_viewed",
            "file_type" to fileType,
            "uploaded_by" to uploadedBy,
            "has_visit" to hasVisit,
        )

    data class DocumentViewFailed(val reason: String) : AnalyticsEvent("document_view_failed", "reason" to reason)

    data class DocumentExpanded(val expanded: Boolean) : AnalyticsEvent("document_expanded", "expanded" to expanded)

    data object DocumentVisitOpened : AnalyticsEvent("document_visit_opened")

    data class DocumentDownloaded(val fileType: String) : AnalyticsEvent("document_downloaded", "file_type" to fileType)

    data object DocumentDownloadFailed : AnalyticsEvent("document_download_failed")

    /** [via]: `swipe` / `viewer`. */
    data class DocumentDeleted(val via: String) : AnalyticsEvent("document_deleted", "via" to via)

    data class DocumentDeleteFailed(val via: String, val reason: String) :
        AnalyticsEvent("document_delete_failed", "via" to via, "reason" to reason)

    // ---- events.md §9: messages ----

    data object ChatsRefreshed : AnalyticsEvent("chats_refreshed")

    data class ChatDoctorSearched(val queryLength: Int, val resultsCount: Int) :
        AnalyticsEvent("chat_doctor_searched", "query_length" to queryLength, "results_count" to resultsCount)

    /** [source]: `list` / `search` / `notification` / `push`. */
    data class ChatOpened(val source: String?, val isNew: Boolean) :
        AnalyticsEvent("chat_opened", "source" to source, "is_new" to isNew)

    /** [type]: `text` / `image` / `voice` / `document`. */
    data class ChatMessageSent(
        val type: String,
        val isFirstInChat: Boolean,
        val hasCaption: Boolean?,
        val durationSBucket: String?,
        val sizeKbBucket: String?,
        val at: String,
    ) : AnalyticsEvent(
            "chat_message_sent",
            "type" to type,
            "is_first_in_chat" to isFirstInChat,
            "has_caption" to hasCaption,
            "duration_s_bucket" to durationSBucket,
            "size_kb_bucket" to sizeKbBucket,
            userProperties = mapOf("has_messaged" to true),
            userPropertiesSetOnce = mapOf("first_message_at" to at),
        )

    /** [stage]: `upload` / `send`. */
    data class ChatMessageFailed(val type: String, val stage: String, val reason: String) :
        AnalyticsEvent("chat_message_failed", "type" to type, "stage" to stage, "reason" to reason)

    data object ChatRecordingStarted : AnalyticsEvent("chat_recording_started")

    data object ChatRecordingFailed : AnalyticsEvent("chat_recording_failed")

    data class ChatAttachmentOpened(val type: String) : AnalyticsEvent("chat_attachment_opened", "type" to type)

    data class ChatVoicePlayed(val isOwn: Boolean?) : AnalyticsEvent("chat_voice_played", "is_own" to isOwn)

    // ---- events.md §10: Shifa AI ----

    data class AiChatStarted(val isNew: Boolean, val savedChats: Int) :
        AnalyticsEvent("ai_chat_started", "is_new" to isNew, "saved_chats" to savedChats)

    /** [input]: `typed` / `voice`; [lengthBucket]: `<50` / `50-200` / `>200`. */
    data class AiMessageSent(val turn: Int, val input: String, val lengthBucket: String, val at: String) :
        AnalyticsEvent(
            "ai_message_sent",
            "turn" to turn,
            "input" to input,
            "length_bucket" to lengthBucket,
            userProperties = mapOf("has_used_ai" to true),
            userPropertiesSetOnce = mapOf("first_ai_message_at" to at),
        )

    data class AiReplyReceived(
        val firstTokenMs: Long?,
        val totalMs: Long,
        val replyCharsBucket: String,
        val isRegenerate: Boolean,
    ) :
        AnalyticsEvent(
                "ai_reply_received",
                "first_token_ms" to firstTokenMs,
                "total_ms" to totalMs,
                "reply_chars_bucket" to replyCharsBucket,
                "is_regenerate" to isRegenerate,
            )

    /** `rate_limit` / `empty` / an API reason. */
    data class AiReplyFailed(val reason: String, val isRegenerate: Boolean) :
        AnalyticsEvent("ai_reply_failed", "reason" to reason, "is_regenerate" to isRegenerate)

    data class AiRateLimited(val turn: Int) : AnalyticsEvent("ai_rate_limited", "turn" to turn)

    data class AiReplyAbandoned(val elapsedMs: Long) : AnalyticsEvent("ai_reply_abandoned", "elapsed_ms" to elapsedMs)

    /** [action]: `copy` / `regenerate` / `share`. */
    data class AiReplyAction(val action: String, val turn: Int) :
        AnalyticsEvent("ai_reply_action", "action" to action, "turn" to turn)

    data class AiVoiceRecorded(val durationS: Long?) : AnalyticsEvent("ai_voice_recorded", "duration_s" to durationS)

    data class AiVoiceTranscribed(val transcribeMs: Long, val transcriptCharsBucket: String) :
        AnalyticsEvent(
            "ai_voice_transcribed",
            "transcribe_ms" to transcribeMs,
            "transcript_chars_bucket" to transcriptCharsBucket,
        )

    /** `permission_denied` / `record_failed` / `rate_limit` / an API reason. */
    data class AiVoiceFailed(val reason: String) : AnalyticsEvent("ai_voice_failed", "reason" to reason)

    data class AiChatDeleted(val messages: Int) : AnalyticsEvent("ai_chat_deleted", "messages" to messages)

    data class AiSearched(val queryLength: Int, val resultsCount: Int) :
        AnalyticsEvent("ai_searched", "query_length" to queryLength, "results_count" to resultsCount)

    data class AiGatedSeen(val subscriptionTier: String?) :
        AnalyticsEvent("ai_gated_seen", "subscription_tier" to subscriptionTier)

    // ---- events.md §11: notifications and push ----

    data class PushPermissionResult(val granted: Boolean) :
        AnalyticsEvent(
            "push_permission_result",
            "granted" to granted,
            userProperties = mapOf("push_granted" to granted),
        )

    data object PushTokenRegistered : AnalyticsEvent("push_token_registered")

    data class PushReceived(val type: String?) : AnalyticsEvent("push_received", "type" to type)

    /**
     * [routedTo]: `appointment` / `live_appointment` / `document` / `documents_tab` / `task` / `tasks_list` / `chat` /
     * `messages_tab` / `bookings_tab` / `inbox`.
     */
    data class PushOpened(val type: String?, val routedTo: String) :
        AnalyticsEvent("push_opened", "type" to type, "routed_to" to routedTo)

    data class NotificationsViewed(val count: Int, val unreadCount: Int) :
        AnalyticsEvent("notifications_viewed", "count" to count, "unread_count" to unreadCount)

    data class NotificationsPageLoaded(val page: Int) : AnalyticsEvent("notifications_page_loaded", "page" to page)

    data object NotificationsRefreshed : AnalyticsEvent("notifications_refreshed")

    /** [filter]: `all` / `appointments` / `tasks` / `documents` / `payments`. */
    data class NotificationFilterSelected(val filter: String) :
        AnalyticsEvent("notification_filter_selected", "filter" to filter)

    /** [ageHoursBucket]: `<1` / `1-24` / `1-7d` / `>7d`. */
    data class NotificationOpened(val type: String, val wasUnread: Boolean, val ageHoursBucket: String) :
        AnalyticsEvent(
            "notification_opened",
            "type" to type,
            "was_unread" to wasUnread,
            "age_hours_bucket" to ageHoursBucket,
        )

    data class NotificationActionTapped(val type: String, val routedTo: String) :
        AnalyticsEvent("notification_action_tapped", "type" to type, "routed_to" to routedTo)

    data class NotificationsAllRead(val unreadCount: Int) :
        AnalyticsEvent("notifications_all_read", "unread_count" to unreadCount)

    // ---- events.md §13: reliability ----

    /** [endpoint]: the path template with ids replaced (`patients/me/appointments/{id}`); null when no request was made. */
    data class ApiError(
        val endpoint: String?,
        val method: String?,
        val status: Int,
        val error: String,
        val code: String?,
    ) :
        AnalyticsEvent(
                "api_error",
                "endpoint" to endpoint,
                "method" to method,
                "status" to status,
                "error" to error,
                "code" to code,
            )

    data class ApiSlow(val endpoint: String, val method: String, val ms: Long) :
        AnalyticsEvent("api_slow", "endpoint" to endpoint, "method" to method, "ms" to ms)

    /** [stage]: `connect` / `stream`. */
    data class AiStreamError(val stage: String, val status: Int) :
        AnalyticsEvent("ai_stream_error", "stage" to stage, "status" to status)

    /** [errorCode]: the platform's WebView error code, never the URL. */
    data class WebviewError(val provider: String?, val errorCode: Int) :
        AnalyticsEvent("webview_error", "provider" to provider, "error_code" to errorCode)

    // ---- events.md §12: profile and settings ----

    data object ProfileRefreshed : AnalyticsEvent("profile_refreshed")

    /** [row]: `edit_profile` / `change_password` / `language` / `privacy` / `debug` / `delete_account` / `logout`. */
    data class ProfileRowTapped(val row: String) : AnalyticsEvent("profile_row_tapped", "row" to row)

    /** [source]: `profile` / `edit_profile`. */
    data class LanguageChanged(val from: String, val to: String, val source: String) :
        AnalyticsEvent("language_changed", "from" to from, "to" to to, "source" to source)

    /** [fields]: the changed fields' names, never their values. */
    data class ProfileUpdated(val fields: List<String>) : AnalyticsEvent("profile_updated", "fields" to fields)

    data class ProfileUpdateFailed(val reason: String) : AnalyticsEvent("profile_update_failed", "reason" to reason)

    data class ProfilePhotoUpdated(val source: String) :
        AnalyticsEvent("profile_photo_updated", "source" to source, userProperties = mapOf("has_photo" to true))

    data class ProfilePhotoFailed(val source: String, val reason: String) :
        AnalyticsEvent("profile_photo_failed", "source" to source, "reason" to reason)

    data object PasswordChanged : AnalyticsEvent("password_changed")

    /** `wrong_current` / `policy` / an API reason. */
    data class PasswordChangeFailed(val reason: String) : AnalyticsEvent("password_change_failed", "reason" to reason)

    data object AccountDeletionStarted : AnalyticsEvent("account_deletion_started")

    data object AccountDeletionCodeResent : AnalyticsEvent("account_deletion_code_resent")

    /** [step]: `request` / `confirm`; [reason]: `email_required` / `invalid_code` / an API reason. */
    data class AccountDeletionFailed(val step: String, val reason: String) :
        AnalyticsEvent("account_deletion_failed", "step" to step, "reason" to reason)

    data object AccountDeleted : AnalyticsEvent("account_deleted")

    data object PrivacyPolicyOpened : AnalyticsEvent("privacy_policy_opened")

    /** [feature]: `map` / `ai_camera` / `ai_photo` / `ai_file`. */
    data class ComingSoonTapped(val feature: String) : AnalyticsEvent("coming_soon_tapped", "feature" to feature)

    /** [screen]: `doctor_profile` / `document_viewer` / `messages` / `notifications`. */
    data class RetryTapped(val screen: String) : AnalyticsEvent("retry_tapped", "screen" to screen)

    // ---- events.md §12: profile ----

    // The "Share usage data" switch. Opted out is the last event sent, opted in the first.
    data object AnalyticsOptedOut : AnalyticsEvent("analytics_opted_out")

    data object AnalyticsOptedIn : AnalyticsEvent("analytics_opted_in")
}

private fun bookedProperties(isVideo: Boolean): Map<String, Any> =
    mapOf("has_booked" to true, "last_visit_type" to if (isVideo) "video" else "in_person")

/** The failure as a fixed reason (privacy.md: never the message). */
fun Throwable.analyticsReason(): String =
    when (this) {
        is ApiException.NetworkError -> "network"
        is ApiException.RateLimit -> "rate_limit"
        is ApiException.Unauthorized -> "unauthorized"
        is ApiException.Forbidden -> "forbidden"
        is ApiException.NotFound -> "not_found"
        is ApiException.Conflict -> "conflict"
        is ApiException.ValidationError -> "validation"
        else -> "server"
    }

/** `declined` / `expired` / `expired_paid_refund` for a dead pay-now booking, null while it lives. */
fun shifa.uz.data.model.Appointment.paymentFailureReason(): String? =
    when {
        !isPaymentDead -> null
        status == "PAYMENT_FAILED" -> "declined"
        isPaid -> "expired_paid_refund"
        else -> "expired"
    }

/** `<500` / `500-2000` / `2000-10000` / `>10000` KB. */
fun sizeKbBucket(bytes: Long): String {
    val kb = bytes / 1024
    return when {
        kb < 500 -> "<500"
        kb < 2_000 -> "500-2000"
        kb < 10_000 -> "2000-10000"
        else -> ">10000"
    }
}

/** `<10` / `10-30` / `30-60` / `>60` seconds. */
fun durationSBucket(seconds: Int): String =
    when {
        seconds < 10 -> "<10"
        seconds < 30 -> "10-30"
        seconds < 60 -> "30-60"
        else -> ">60"
    }

/** `<50` / `50-200` / `>200` characters, for text whose content never travels. */
fun charsBucket(length: Int): String =
    when {
        length < 50 -> "<50"
        length < 200 -> "50-200"
        else -> ">200"
    }

/** `<300` / `300-1000` / `>1000` characters of a reply. */
fun replyCharsBucket(length: Int): String =
    when {
        length < 300 -> "<300"
        length < 1_000 -> "300-1000"
        else -> ">1000"
    }

/** `<1` / `1-24` / `1-7d` / `>7d` hours since [at]. */
fun ageHoursBucket(
    at: Instant,
    now: Instant,
): String {
    val hours = (now - at).inWholeHours
    return when {
        hours < 1 -> "<1"
        hours < 24 -> "1-24"
        hours < 24 * 7 -> "1-7d"
        else -> ">7d"
    }
}

/** UZS so'm buckets (privacy.md §2: never the exact amount); other currencies are `foreign`. */
fun amountBucket(
    amountMinor: Long?,
    currency: String?,
): String? {
    if (amountMinor == null || currency.isNullOrBlank()) return null
    if (!currency.equals("UZS", ignoreCase = true)) return "foreign"
    val som = amountMinor / 100
    return when {
        som < 50_000 -> "<50k"
        som < 150_000 -> "50k-150k"
        som < 500_000 -> "150k-500k"
        else -> ">500k"
    }
}

/** Whole hours from [now] to [start]; negative once started. */
fun hoursAhead(
    start: Instant?,
    now: Instant,
): Int? = start?.let { (it - now).inWholeHours.toInt() }

fun daysAhead(
    start: Instant?,
    now: Instant,
): Int? = start?.let { (it - now).inWholeDays.toInt() }

fun hourOfDay(
    start: Instant?,
    zone: TimeZone,
): Int? = start?.toLocalDateTime(zone)?.hour
