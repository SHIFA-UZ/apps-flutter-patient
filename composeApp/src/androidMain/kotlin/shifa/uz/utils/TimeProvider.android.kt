package shifa.uz.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual fun getCurrentInstant(): Instant = Clock.System.now()
