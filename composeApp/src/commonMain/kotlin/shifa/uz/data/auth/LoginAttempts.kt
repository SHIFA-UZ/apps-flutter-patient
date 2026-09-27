package shifa.uz.data.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import shifa.uz.utils.getCurrentInstant

const val MAX_LOGIN_ATTEMPTS = 5
private const val LOCKOUT_MS = 5 * 60_000L
private const val KEY_FAILED = "login_failed_attempts"
private const val KEY_LOCKED_UNTIL = "login_lockout_until_epoch_ms"

/** 05 §6.2: 5 failed logins lock sign in for 5 minutes. Persisted, so a restart doesn't reset it. */
class LoginAttempts(
    private val settings: Settings = Settings(),
    private val now: () -> Long = { getCurrentInstant().toEpochMilliseconds() },
) {
    /** 0 when not locked. An expired lock clears both counters. */
    fun lockoutRemainingMs(): Long {
        val until = settings.getLongOrNull(KEY_LOCKED_UNTIL) ?: return 0
        val left = until - now()
        if (left <= 0) clear()
        return left.coerceAtLeast(0)
    }

    /** Whole minutes left, rounded up to 1..5; 0 when not locked. */
    fun lockedMinutes(): Int {
        val ms = lockoutRemainingMs()
        return if (ms == 0L) 0 else ((ms + 59_999) / 60_000).toInt().coerceIn(1, 5)
    }

    fun remainingAttempts(): Int =
        if (lockoutRemainingMs() > 0) {
            0
        } else {
            (
                MAX_LOGIN_ATTEMPTS -
                    settings.getInt(
                        KEY_FAILED,
                        0,
                    )
            ).coerceIn(0, MAX_LOGIN_ATTEMPTS)
        }

    fun recordFailure() {
        if (lockoutRemainingMs() > 0) return
        val failed = settings.getInt(KEY_FAILED, 0) + 1
        settings[KEY_FAILED] = failed
        if (failed >= MAX_LOGIN_ATTEMPTS) settings[KEY_LOCKED_UNTIL] = now() + LOCKOUT_MS
    }

    fun clear() {
        settings.remove(KEY_FAILED)
        settings.remove(KEY_LOCKED_UNTIL)
    }
}
