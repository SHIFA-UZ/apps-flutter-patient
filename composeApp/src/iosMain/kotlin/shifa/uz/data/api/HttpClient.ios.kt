package shifa.uz.data.api

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.Logger
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual fun createHttpEngine(): HttpClientEngine = Darwin.create()

@OptIn(ExperimentalNativeApi::class)
actual val httpLogger: Logger = if (Platform.isDebugBinary) Logger.DEFAULT else Logger.EMPTY
