package shifa.uz.data.api

object ApiConfig {
    // 15 §1.1: every build uses production. Endpoint paths are relative to /api/.
    const val BASE_URL = "https://shifa-doc-backend-mvp-production.up.railway.app/api/"
    const val TIMEOUT_MS = 30_000L

    object Endpoints {
        const val LOGIN = "auth/login"
        const val PROFILE = "patients/me/profile"
    }

    // 01 §4.2: sent without a token. A request matches when its path ends with one of these.
    val PUBLIC_AUTH_PATHS =
        listOf(
            "/auth/login",
            "/auth/register",
            "/auth/register-patient",
            "/auth/register-clinic-staff",
            "/auth/verify-key",
            "/auth/check-existing-patient",
            "/auth/check-existing-doctor",
            "/auth/check-identifier",
            "/auth/create-patient-for-doctor",
            "/auth/send-email-otp",
            "/auth/send-sms-otp",
            "/auth/verify",
            "/auth/verify-email-otp",
            "/auth/send-login-otp",
            "/auth/send-forgot-password-otp",
            "/auth/forgot-password-reset",
        )
}
