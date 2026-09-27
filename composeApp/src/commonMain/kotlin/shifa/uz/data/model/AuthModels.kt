package shifa.uz.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

/** A1. A missing token decodes as blank and is rejected by the repository. */
@Serializable
data class LoginResponse(
    val token: String = "",
    val forcePasswordReset: Boolean = false,
)
