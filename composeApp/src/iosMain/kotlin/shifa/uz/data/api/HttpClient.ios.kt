package shifa.uz.data.api

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

actual fun createHttpEngine(): HttpClientEngine = Darwin.create()
