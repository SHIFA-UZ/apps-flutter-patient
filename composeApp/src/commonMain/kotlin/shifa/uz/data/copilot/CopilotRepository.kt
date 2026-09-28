package shifa.uz.data.copilot

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import shifa.uz.data.api.ApiConfig
import shifa.uz.data.api.COPILOT_FAILED
import shifa.uz.data.api.TOO_MANY_REQUESTS
import shifa.uz.data.api.apiJson
import shifa.uz.data.api.httpClient
import shifa.uz.data.api.networkErrorMessage
import shifa.uz.data.api.sanitizeError
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CopilotError
import shifa.uz.data.model.CopilotMessage
import shifa.uz.data.model.CopilotStreamRequest

private const val RATE_LIMIT = "RATE_LIMIT"

// Open for the ViewModel test's fake: Mockito's answers can't return a Result from a suspend fun.
open class CopilotRepository(
    private val client: HttpClient = httpClient,
) {
    /** X1 (09 §2.1): each SSE token goes to [onToken] as it arrives; the result is the whole reply, trimmed. */
    open suspend fun stream(
        messages: List<CopilotMessage>,
        language: String,
        onToken: (String) -> Unit,
    ): Result<String> =
        try {
            client.preparePost(ApiConfig.Endpoints.COPILOT_STREAM) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Text.EventStream)
                setBody(CopilotStreamRequest(messages, language))
            }.execute { response ->
                val status = response.status.value
                if (status != 200) {
                    val text = response.bodyAsText()
                    val error = decode(text)
                    if (status == 429 || error?.code == RATE_LIMIT) throw ApiException.RateLimit(TOO_MANY_REQUESTS)
                    throw ApiException.of(
                        status,
                        sanitizeError(status, error?.message ?: text, COPILOT_FAILED, auth = false),
                    )
                }
                val reply = StringBuilder()
                readEvents(response.bodyAsChannel()) {
                    reply.append(it)
                    onToken(it)
                }
                Result.success(reply.toString().trim())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ApiException.NetworkError(networkErrorMessage(e)))
        }

    // 09 §8, exactly: a blank line ends an event, `data:` keeps its leading space, data left at EOF still counts.
    private suspend fun readEvents(
        channel: ByteReadChannel,
        emit: (String) -> Unit,
    ) {
        var event = ""
        val data = mutableListOf<String>()
        while (true) {
            val line = channel.readUTF8Line() ?: break
            when {
                line.isEmpty() -> {
                    val text = data.joinToString("\n")
                    val name = event
                    event = ""
                    data.clear()
                    if (name == "error" && text.isNotBlank()) throw sseError(text)
                    if (text.isNotEmpty()) emit(text)
                }
                line.startsWith("event:") -> event = line.substring(6).trim()
                line.startsWith("data:") -> data += line.substring(5)
            }
        }
        data.joinToString("\n").takeIf { it.isNotEmpty() }?.let(emit)
    }

    // Other codes (safety…) carry backend text, so they show as the generic error (01 §8).
    private fun sseError(data: String) =
        when (decode(data)?.code) {
            RATE_LIMIT -> ApiException.RateLimit(TOO_MANY_REQUESTS)
            else -> ApiException.ServerError(COPILOT_FAILED)
        }

    private fun decode(text: String) = runCatching { apiJson.decodeFromString<CopilotError>(text) }.getOrNull()
}
