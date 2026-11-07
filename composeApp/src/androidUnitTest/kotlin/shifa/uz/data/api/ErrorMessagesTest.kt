package shifa.uz.data.api

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorVideoCallHasEnded
import shifa.composeapp.generated.resources.errorVideoCallNotYetAvailable
import shifa.composeapp.generated.resources.errorVideoCallPaymentRequired
import shifa.uz.data.model.ApiException

class ErrorMessagesTest {
    @Test
    fun `a known message wins, then the auth account texts, then a known code, then the default`() {
        assertThat(
            sanitizeError(429, "Too many verification requests. Please try again later.", "x", false, "RATE_LIMITED"),
        )
            .isEqualTo("Too many verification requests. Please try again later.")
        assertThat(sanitizeError(401, "Session invalid", LOGIN_FAILED, auth = true, code = "SESSION_INVALID"))
            .isEqualTo("Invalid credentials")
        assertThat(
            sanitizeError(401, "Session invalid", FAILED_TO_LOAD_NOTIFICATIONS, auth = false, code = "SESSION_INVALID"),
        )
            .isEqualTo("Invalid or expired token")
        assertThat(sanitizeError(403, "Disabled by an admin", FAILED_TO_LOAD_NOTIFICATIONS, false, "ACCOUNT_DISABLED"))
            .isEqualTo("Account is disabled")
        assertThat(
            sanitizeError(500, "java.lang.NullPointerException", FAILED_TO_LOAD_NOTIFICATIONS, false, "INTERNAL"),
        )
            .isEqualTo(FAILED_TO_LOAD_NOTIFICATIONS)
        assertThat(sanitizeError(429, "Rate limit exceeded for 10.0.0.1", FAILED_TO_LOAD_NOTIFICATIONS, false, null))
            .isEqualTo(TOO_MANY_REQUESTS)
    }

    @Test
    fun `V1's refusal codes give the join window and payment texts whatever the message`() {
        fun shown(
            status: Int,
            code: String,
        ) = ApiException.of(status, sanitizeError(status, "Too early: 09:55", FAILED_TO_START_VIDEO_CALL, false, code))
            .toUserMessage()
        assertThat(shown(403, "VIDEO_TOO_EARLY")).isEqualTo(Res.string.errorVideoCallNotYetAvailable)
        assertThat(shown(403, "VIDEO_TOO_LATE")).isEqualTo(Res.string.errorVideoCallHasEnded)
        assertThat(shown(402, "VIDEO_PAYMENT_REQUIRED")).isEqualTo(Res.string.errorVideoCallPaymentRequired)
        // Without a code (production today), the message still decides.
        assertThat(
            sanitizeError(
                403,
                "Video call has ended. The join window closes 15 minutes after the appointment end.",
                FAILED_TO_START_VIDEO_CALL,
                false,
            ),
        ).isEqualTo("Video call has ended. The join window closes 15 minutes after the appointment end.")
        assertThat(
            sanitizeError(403, "Forbidden", FAILED_TO_START_VIDEO_CALL, false),
        ).isEqualTo(FAILED_TO_START_VIDEO_CALL)
    }
}
