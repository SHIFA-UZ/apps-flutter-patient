package shifa.uz.data.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import shifa.uz.data.model.CopilotChat

private const val MAX_MESSAGES = 60

// ponytail: the store is read whole on every access; a database if chats need to outgrow this.
private const val MAX_CHATS = 20

private val json = Json { ignoreUnknownKeys = true }

/** Shifa AI chats (09 §6), newest first. In the secure store (health data); cleared on logout (00 bug 4). */
class CopilotStorage(
    private val settings: Settings = secureSettings,
) {
    /** A store that doesn't decode is dropped, not a crash. */
    fun load(): List<CopilotChat> =
        settings.getStringOrNull(KEY)?.let {
            runCatching { json.decodeFromString<List<CopilotChat>>(it) }.getOrNull()
        }.orEmpty()

    /** Keeps the newest [MAX_CHATS], each with its last [MAX_MESSAGES] non-blank turns (09 Q2). Returns what it kept. */
    fun save(chats: List<CopilotChat>): List<CopilotChat> {
        val kept =
            chats.take(MAX_CHATS).map { chat ->
                chat.copy(messages = chat.messages.filter { it.content.isNotBlank() }.takeLast(MAX_MESSAGES))
            }
        settings[KEY] = json.encodeToString(kept)
        return kept
    }

    fun clear() = settings.remove(KEY)

    private companion object {
        const val KEY = "copilot_chats_v1"
    }
}
