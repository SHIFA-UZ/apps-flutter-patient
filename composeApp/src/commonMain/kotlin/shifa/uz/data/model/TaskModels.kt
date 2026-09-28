package shifa.uz.data.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable

/** T1 item, the keys the app reads (15 §5.11). `startDate` is required: items without it are skipped, as in Flutter. */
@Serializable
data class RemoteCareTask(
    val id: String = "",
    val taskName: String = "",
    val description: String? = null,
    val category: String = "OTHER", // VITAL, EXERCISE, MEDICATION, OTHER
    val status: String = "ACTIVE", // DRAFT, ACTIVE, COMPLETED, EXPIRED, CANCELLED
    val startDate: String,
    val inputType: String = "TEXT", // NUMERIC, TEXT, BOOLEAN
    val inputLabel: String? = null,
    val notesRequired: Boolean = false,
    val notesLabel: String? = null,
    val progress: TaskProgress? = null,
) {
    /** 12 §3.1: the Open tab, which keeps EXPIRED. */
    val isOpen get() = status != "COMPLETED" && status != "CANCELLED"

    /** 12 §3.1: the Completed tab. An EXPIRED task with nothing pending is in both (quirk 1, kept). */
    val isCompleted get() =
        status == "COMPLETED" ||
            (status == "EXPIRED" && progress != null && progress.pendingCheckIns == 0 && progress.totalCheckIns > 0)
}

@Serializable
data class TaskProgress(
    val totalCheckIns: Int = 0,
    val completedCheckIns: Int = 0,
    val pendingCheckIns: Int = 0,
)

/** T3 item. `scheduledDate` is a local `yyyy-MM-dd`, `scheduledTime` a local `HH:mm[:ss]` (12 §5.4). */
@Serializable
data class TaskCheckIn(
    val id: Long,
    val scheduledDate: String,
    val scheduledTime: String? = null,
    val status: String = "PENDING", // PENDING, COMPLETED, MISSED; anything else reads as PENDING (12 §5.1)
) {
    val isPending get() = status.uppercase() != "COMPLETED" && status.uppercase() != "MISSED"

    /** The device's wall clock, as in Flutter (the server's zone is unverified). Null if it doesn't parse. */
    fun scheduledAt(zone: TimeZone): Instant? =
        runCatching {
            val time = scheduledTime?.let { LocalTime.parse(it.take(5)) } ?: LocalTime(0, 0)
            LocalDate.parse(scheduledDate.take(10)).atTime(time).toInstant(zone)
        }.getOrNull()
}

/** T4 body; nulls are left out (`explicitNulls = false`), as in Flutter. */
@Serializable
data class CheckInRequest(
    val checkInId: Long,
    val numericValue: Double? = null,
    val textValue: String? = null,
    val booleanValue: Boolean? = null,
    val notes: String? = null,
)
