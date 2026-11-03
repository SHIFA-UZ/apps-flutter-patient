package shifa.uz.ui.shifaai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.StringResource
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.copilotDailyLimit
import shifa.composeapp.generated.resources.copilotTranscribeError
import shifa.composeapp.generated.resources.errorRecordingVoice
import shifa.composeapp.generated.resources.microphonePermissionDenied
import shifa.uz.data.analytics.Analytics
import shifa.uz.data.analytics.AnalyticsEvent
import shifa.uz.data.analytics.AnalyticsTracker
import shifa.uz.data.analytics.analyticsReason
import shifa.uz.data.analytics.charsBucket
import shifa.uz.data.analytics.replyCharsBucket
import shifa.uz.data.api.toUserMessage
import shifa.uz.data.copilot.CopilotRepository
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.CopilotChat
import shifa.uz.data.model.CopilotMessage
import shifa.uz.data.storage.AppLanguage
import shifa.uz.data.storage.CopilotStorage
import shifa.uz.data.storage.languageStorage
import shifa.uz.utils.getCurrentInstant

/** 09 §5.1 (AI-01): the chat list and the open chat. Lives in the tab shell, so the list survives tab switches. */
class ShifaAiViewModel(
    private val copilot: CopilotRepository = CopilotRepository(),
    private val storage: CopilotStorage = CopilotStorage(),
    private val language: StateFlow<AppLanguage> = languageStorage.language,
    private val analytics: AnalyticsTracker = Analytics,
    private val now: () -> Long = { getCurrentInstant().toEpochMilliseconds() },
) : ViewModel() {
    // events.md §10: timings and how the question came in.
    private var askedAt = 0L
    private var firstTokenAt: Long? = null
    private var recordingSince: Long? = null
    private var viaVoice = false
    private val _state = MutableStateFlow(ShifaAiUiState(chats = storage.load()))
    val state: StateFlow<ShifaAiUiState> = _state.asStateFlow()

    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut: Flow<Unit> = _signedOut.receiveAsFlow()

    private var reply: Job? = null
    private var transcription: Job? = null

    private fun update(transform: ShifaAiUiState.() -> ShifaAiUiState) {
        _state.value = _state.value.transform()
    }

    fun search(query: String) = update { copy(query = query) }

    fun edit(input: String) = update { copy(input = input) }

    /** Opens a saved chat, or with no [id] a new one, saved once it has a message. Reopening lifts a rate limit. */
    fun open(id: String? = null) {
        val chat = _state.value.chats.firstOrNull { it.id == id } ?: CopilotChat(now().toString())
        analytics.track(
            AnalyticsEvent.AiChatStarted(isNew = chat.messages.isEmpty(), savedChats = _state.value.chats.size),
        )
        update {
            copy(
                chat = chat,
                input = "",
                error = null,
                notice = null,
                rateLimited = false,
                recording = false,
                isTranscribing = false,
            )
        }
    }

    /** Leaving stops the reply (09 §8); its question stays, unanswered. */
    fun close() {
        if (_state.value.streaming != null) analytics.track(AnalyticsEvent.AiReplyAbandoned(now() - askedAt))
        reply?.cancel()
        transcription?.cancel()
        update {
            copy(chat = null, streaming = null, error = null, notice = null, recording = false, isTranscribing = false)
        }
    }

    fun delete() {
        val chat = _state.value.chat ?: return
        val id = chat.id
        analytics.track(AnalyticsEvent.AiChatDeleted(chat.messages.size))
        close()
        update { copy(chats = storage.save(chats.filterNot { it.id == id })) }
    }

    /** For what isn't built yet (attachments). */
    fun showNotice(notice: StringResource) = update { copy(notice = notice, error = null) }

    fun showError(error: StringResource) = update { copy(error = error, notice = null) }

    fun onRecording() {
        recordingSince = now()
        update { copy(recording = true, error = null, notice = null) }
    }

    /** The recorder refused to start (events.md §10). */
    fun onRecordFailed() {
        analytics.track(AnalyticsEvent.AiVoiceFailed("record_failed"))
        showError(Res.string.errorRecordingVoice)
    }

    /** The microphone permission was refused (events.md §10). */
    fun onMicDenied() {
        analytics.track(AnalyticsEvent.AiVoiceFailed("permission_denied"))
        showError(Res.string.microphonePermissionDenied)
    }

    /** Copy or Share on [reply]; the turn is that reply's, not the chat's last. Regenerate tracks itself. */
    fun onReplyAction(
        action: String,
        reply: CopilotMessage,
    ) {
        val messages = _state.value.chat?.messages.orEmpty()
        val at = messages.indexOfFirst { it === reply }.takeIf { it >= 0 } ?: messages.indexOf(reply)
        val turn = if (at < 0) turn() else messages.take(at).count { it.isUser }
        analytics.track(AnalyticsEvent.AiReplyAction(action, turn))
    }

    private fun turn() = _state.value.chat?.messages?.count { it.isUser } ?: 0

    /** X2: the transcript is sent as the question at once (09 §5.7 had it fill the input; the user wants it sent). */
    fun transcribe(audio: ByteArray?) {
        update { copy(recording = false) }
        analytics.track(AnalyticsEvent.AiVoiceRecorded(recordingSince?.let { (now() - it) / 1_000 }))
        recordingSince = null
        if (audio == null) {
            analytics.track(AnalyticsEvent.AiVoiceFailed("record_failed"))
            return showError(Res.string.errorRecordingVoice)
        }
        update { copy(isTranscribing = true, error = null, notice = null) }
        val started = now()
        transcription =
            viewModelScope.launch {
                copilot.transcribe(audio, language.value.whisperCode).fold(
                    onSuccess = {
                        analytics.track(AnalyticsEvent.AiVoiceTranscribed(now() - started, charsBucket(it.length)))
                        update { copy(isTranscribing = false, input = it.ifBlank { input }) }
                        if (it.isNotBlank()) {
                            viaVoice = true
                            send()
                            viaVoice = false
                        }
                    },
                    onFailure = {
                        val limited = it is ApiException.RateLimit
                        analytics.track(
                            AnalyticsEvent.AiVoiceFailed(if (limited) "rate_limit" else it.analyticsReason()),
                        )
                        update {
                            copy(
                                isTranscribing = false,
                                rateLimited = rateLimited || limited,
                                error =
                                    if (limited) Res.string.copilotDailyLimit else Res.string.copilotTranscribeError,
                            )
                        }
                        if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                    },
                )
            }
    }

    /** X1: the reply streams into [ShifaAiUiState.streaming], then joins the chat. */
    fun send() {
        val state = _state.value
        val open = state.chat ?: return
        if (!state.canSend) return
        update { copy(input = "") }
        val text = state.input.trim()
        analytics.track(
            AnalyticsEvent.AiMessageSent(
                turn = open.messages.count { it.isUser } + 1,
                input = if (viaVoice) "voice" else "typed",
                lengthBucket = charsBucket(text.length),
                at = Instant.fromEpochMilliseconds(now()).toString(),
            ),
        )
        ask(put(open.copy(messages = open.messages + CopilotMessage(CopilotMessage.USER, text, now()))))
    }

    /** Figma's refresh on the last reply (1673:14621): drop it and ask X1 again with the same question. */
    fun regenerate() {
        val state = _state.value
        val open = state.chat ?: return
        if (state.isSending || state.rateLimited || open.messages.lastOrNull()?.isUser != false) return
        analytics.track(AnalyticsEvent.AiReplyAction("regenerate", turn()))
        ask(put(open.copy(messages = open.messages.dropLast(1))), isRegenerate = true)
    }

    private fun ask(
        asked: CopilotChat,
        isRegenerate: Boolean = false,
    ) {
        update { copy(streaming = "", error = null, notice = null) }
        askedAt = now()
        firstTokenAt = null
        reply =
            viewModelScope.launch {
                copilot.stream(
                    asked.messages,
                    language.value.copilotCode,
                ) {
                    if (firstTokenAt == null) firstTokenAt = now()
                    update { copy(streaming = streaming?.plus(it)) }
                }.fold(
                    onSuccess = {
                        update { copy(streaming = null) }
                        analytics.track(
                            if (it.isEmpty()) {
                                AnalyticsEvent.AiReplyFailed("empty", isRegenerate)
                            } else {
                                AnalyticsEvent.AiReplyReceived(
                                    firstTokenMs = firstTokenAt?.let { t -> t - askedAt },
                                    totalMs = now() - askedAt,
                                    replyCharsBucket = replyCharsBucket(it.length),
                                    isRegenerate = isRegenerate,
                                )
                            },
                        )
                        // 09 Q14: an empty reply isn't a message.
                        if (it.isNotEmpty()) {
                            put(
                                asked.copy(
                                    messages = asked.messages + CopilotMessage(CopilotMessage.ASSISTANT, it, now()),
                                ),
                            )
                        }
                    },
                    onFailure = {
                        val limited = it is ApiException.RateLimit
                        analytics.track(
                            AnalyticsEvent.AiReplyFailed(
                                if (limited) "rate_limit" else it.analyticsReason(),
                                isRegenerate,
                            ),
                        )
                        if (limited) analytics.track(AnalyticsEvent.AiRateLimited(turn()))
                        update {
                            copy(
                                streaming = null,
                                rateLimited = limited,
                                error = if (limited) Res.string.copilotDailyLimit else it.toUserMessage(),
                            )
                        }
                        if (it is ApiException.Unauthorized) _signedOut.send(Unit)
                    },
                )
            }
    }

    // Saves [chat] at the top of the list; returns it as stored.
    private fun put(chat: CopilotChat): CopilotChat {
        val saved =
            storage.save(
                listOf(chat.copy(updatedAt = now())) + _state.value.chats.filterNot { it.id == chat.id },
            )
        update { copy(chats = saved, chat = saved.first().takeIf { it.id == this.chat?.id } ?: this.chat) }
        return saved.first()
    }
}

// 09 §2.5: Whisper's hint; German gets English.
private val AppLanguage.whisperCode get() = copilotCode.lowercase()

// 09 §2: the backend's OutputLanguage; German gets English.
private val AppLanguage.copilotCode
    get() =
        when (this) {
            AppLanguage.Russian -> "RU"
            AppLanguage.Uzbek -> "UZ"
            else -> "EN"
        }
