package shifa.uz.data.api

object ApiConfig {
    // TODO: set new API base URL
    const val BASE_URL = "http://localhost:8080"
    const val TIMEOUT_MS = 300_000L

    object Endpoints {
        const val REFRESH_TOKEN = "/api/refresh-token"
    }
}
