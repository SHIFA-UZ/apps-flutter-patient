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
