package shifa.uz.data.api

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
