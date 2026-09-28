package shifa.uz.data.api

object ApiConfig {
    // 15 §1.1: every build uses production. Endpoint paths are relative to /api/.
    const val BASE_URL = "https://shifa-doc-backend-mvp-production.up.railway.app/api/"
    const val TIMEOUT_MS = 30_000L

    object Endpoints {
        const val LOGIN = "auth/login"
        const val CHECK_IDENTIFIER = "auth/check-identifier"
        const val SEND_EMAIL_OTP = "auth/send-email-otp"
        const val SEND_SMS_OTP = "auth/send-sms-otp"
        const val REGISTER_PATIENT = "auth/register-patient"
        const val CREATE_PATIENT_FOR_DOCTOR = "auth/create-patient-for-doctor"
        const val SEND_FORGOT_PASSWORD_OTP = "auth/send-forgot-password-otp"
        const val FORGOT_PASSWORD_RESET = "auth/forgot-password-reset"
        const val RESET_PASSWORD = "auth/reset-password"
        const val PROFILE = "patients/me/profile"
        const val PATIENT_PHOTO = "patients/me/photo"
        const val DELETE_ACCOUNT_REQUEST = "patients/me/delete-account/request"
        const val DELETE_ACCOUNT_CONFIRM = "patients/me/delete-account/confirm"
        const val FCM_TOKEN = "patients/me/fcm-token"
        const val APPOINTMENTS = "patients/me/appointments"
        const val MY_TASKS = "tasks/my-tasks"
        const val UNREAD_NOTIFICATIONS_COUNT = "notifications/unread/count"
        const val DOCUMENTS = "patients/me/documents"

        fun doctor(id: String) = "public/doctors/$id"

        fun document(id: String) = "patients/me/documents/$id"
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
