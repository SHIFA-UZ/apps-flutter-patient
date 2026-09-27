package shifa.uz.data.model

import kotlinx.serialization.Serializable

/** N2. */
@Serializable
data class UnreadCount(
    val count: Int = 0,
)
