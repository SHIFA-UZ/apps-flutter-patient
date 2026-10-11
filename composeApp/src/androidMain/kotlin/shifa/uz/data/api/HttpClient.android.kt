package shifa.uz.data.api

import android.util.Log
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.MessageLengthLimitingLogger
import shifa.uz.BuildConfig

actual fun createHttpEngine(): HttpClientEngine = OkHttp.create()

// Ktor's DEFAULT (and its ANDROID in 2.3) goes through SLF4J, which has no binding here, so nothing showed.
actual val httpLogger: Logger = if (BuildConfig.DEBUG) MessageLengthLimitingLogger(delegate = Logcat) else Logger.EMPTY

private object Logcat : Logger {
    // Plain JVM unit tests only have android.jar's stub Log, which throws.
    override fun log(message: String) {
        runCatching { Log.d("HttpClient", message) }.onFailure { println(message) }
    }
}
