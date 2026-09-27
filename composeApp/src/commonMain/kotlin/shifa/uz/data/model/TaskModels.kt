package shifa.uz.data.model

import kotlinx.serialization.Serializable

/** T1 item, the keys Home reads (15 §5.11). `startDate` is required: items without it are skipped, as in Flutter. */
@Serializable
data class RemoteCareTask(
    val id: String = "",
    val taskName: String = "",
    val description: String? = null,
    val category: String = "OTHER", // VITAL, EXERCISE, MEDICATION, OTHER
    val status: String = "ACTIVE", // DRAFT, ACTIVE, COMPLETED, EXPIRED, CANCELLED
    val startDate: String,
) {
    /** 12: the Open tab, which keeps EXPIRED. */
    val isOpen get() = status != "COMPLETED" && status != "CANCELLED"
}
