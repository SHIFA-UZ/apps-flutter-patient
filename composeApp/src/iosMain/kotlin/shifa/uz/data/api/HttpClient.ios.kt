package shifa.uz.data.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.EMPTY
import io.ktor.client.plugins.logging.Logger
import shifa.uz.utils.isDebugBuild

actual fun createHttpEngine(): HttpClientEngine = Darwin.create()

actual val httpLogger: Logger = if (isDebugBuild) Logger.DEFAULT else Logger.EMPTY
