package shifa.uz.utils

// for android local development
expect fun isAndroidEmulator(): Boolean

fun getAdjustedBaseUrl(baseUrl: String): String {
    return if ((baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) && isAndroidEmulator()) {
        val adjusted = baseUrl.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")
        adjusted
    } else {
        baseUrl
    }
}
