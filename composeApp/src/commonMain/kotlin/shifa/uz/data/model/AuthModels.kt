package shifa.uz.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenRefreshResponse(
    val token: String,
    val expiresIn: Int,
    val message: String? = null,
)
