package shifa.uz.utils

import kotlinx.datetime.Instant
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentInstant(): Instant {
    val timeInSeconds = NSDate().timeIntervalSince1970
    return Instant.fromEpochMilliseconds((timeInSeconds * 1000).toLong())
}
