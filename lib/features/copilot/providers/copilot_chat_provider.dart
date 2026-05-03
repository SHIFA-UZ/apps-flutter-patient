import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';
import 'package:shifa_patient_app_v1/features/copilot/data/copilot_api.dart';
import 'package:shifa_patient_app_v1/features/copilot/data/copilot_memory_store.dart';
import 'package:shifa_patient_app_v1/features/copilot/providers/copilot_api_provider.dart';

class CopilotMessage {
  final String role;
  final String content;

  CopilotMessage({required this.role, required this.content});
}

class CopilotChatState {
  final List<CopilotMessage> messages;
  final String? streamingText;
  final bool isSending;
  final bool isThinking;
  final bool isAnalyzingSymptoms;
  final bool isFindingDoctors;
  final String? lastError;
  final String contextSummary;
  final List<String> memorySymptoms;
  final List<String> memorySuspectedConditions;
  final List<String> memoryBookingDecisions;
  /// `null` = doctor suggestions not loaded for this session; non-empty = suggested ids; empty = search returned no doctors.
  final List<String>? lastSuggestedDoctorIds;

  CopilotChatState({
    this.messages = const [],
    this.streamingText,
    this.isSending = false,
    this.isThinking = false,
    this.isAnalyzingSymptoms = false,
    this.isFindingDoctors = false,
    this.lastError,
    this.contextSummary = '',
    this.memorySymptoms = const [],
    this.memorySuspectedConditions = const [],
    this.memoryBookingDecisions = const [],
    this.lastSuggestedDoctorIds,
  });

  CopilotChatState copyWith({
    List<CopilotMessage>? messages,
    String? streamingText,
    bool? isSending,
    bool? isThinking,
    bool? isAnalyzingSymptoms,
    bool? isFindingDoctors,
    String? lastError,
    String? contextSummary,
    List<String>? memorySymptoms,
    List<String>? memorySuspectedConditions,
    List<String>? memoryBookingDecisions,
    List<String>? lastSuggestedDoctorIds,
    bool clearStreaming = false,
    bool clearError = false,
  }) {
    return CopilotChatState(
      messages: messages ?? this.messages,
      streamingText: clearStreaming ? null : (streamingText ?? this.streamingText),
      isSending: isSending ?? this.isSending,
      isThinking: isThinking ?? this.isThinking,
      isAnalyzingSymptoms: isAnalyzingSymptoms ?? this.isAnalyzingSymptoms,
      isFindingDoctors: isFindingDoctors ?? this.isFindingDoctors,
      lastError: clearError ? null : (lastError ?? this.lastError),
      contextSummary: contextSummary ?? this.contextSummary,
      memorySymptoms: memorySymptoms ?? this.memorySymptoms,
      memorySuspectedConditions: memorySuspectedConditions ?? this.memorySuspectedConditions,
      memoryBookingDecisions: memoryBookingDecisions ?? this.memoryBookingDecisions,
      lastSuggestedDoctorIds: lastSuggestedDoctorIds ?? this.lastSuggestedDoctorIds,
    );
  }
}

class CopilotChatNotifier extends StateNotifier<CopilotChatState> {
  CopilotChatNotifier(this._ref) : super(CopilotChatState()) {
    _hydrateMemory();
  }

  final Ref _ref;
  final CopilotMemoryStore _memory = CopilotMemoryStore();

  CopilotApi get _api => _ref.read(copilotApiProvider);

  Future<void> _hydrateMemory() async {
    final loaded = await _memory.load();
    if (loaded.messages.isEmpty && loaded.structuredState.isEmpty) return;
    final hydrated = loaded.messages
        .map((m) => CopilotMessage(role: m['role'] ?? 'assistant', content: m['content'] ?? ''))
        .toList();
    state = state.copyWith(
      messages: hydrated,
      contextSummary: loaded.summary,
      memorySymptoms: (loaded.structuredState['symptoms'] as List<dynamic>? ?? const []).map((e) => e.toString()).toList(),
      memorySuspectedConditions:
          (loaded.structuredState['suspectedConditions'] as List<dynamic>? ?? const []).map((e) => e.toString()).toList(),
      memoryBookingDecisions:
          (loaded.structuredState['bookingDecisions'] as List<dynamic>? ?? const []).map((e) => e.toString()).toList(),
    );
  }

  Future<void> _persistMemory() async {
    final compact = state.messages
        .take(state.messages.length > 60 ? 60 : state.messages.length)
        .map((m) => {'role': m.role, 'content': m.content})
        .toList();
    await _memory.save(
      messages: compact,
      summary: state.contextSummary,
      structuredState: {
        'symptoms': state.memorySymptoms,
        'suspectedConditions': state.memorySuspectedConditions,
        'bookingDecisions': state.memoryBookingDecisions,
      },
    );
  }

  String _rebuildSummary(List<CopilotMessage> msgs) {
    final users = msgs.where((m) => m.role == 'user').map((m) => m.content.trim()).where((t) => t.isNotEmpty).toList();
    final recent = users.length > 6 ? users.sublist(users.length - 6) : users;
    if (recent.isEmpty) return state.contextSummary;
    return recent.join(' | ').replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  List<String> _extractSymptomsFromText(String text) {
    final t = text.toLowerCase();
    final lex = <String>[
      'chest pain', 'headache', 'fever', 'cough', 'nausea', 'vomit', 'rash', 'fatigue', 'dizziness',
      'back pain', 'stomach pain', 'shortness of breath'
    ];
    return lex.where((s) => t.contains(s)).toList();
  }

  void _appendAssistantIfNew(String content) {
    final t = content.trim();
    if (t.isEmpty) return;
    final last = state.messages.isNotEmpty ? state.messages.last : null;
    if (last != null && last.role == 'assistant' && last.content.trim() == t) return;
    state = state.copyWith(
      messages: [...state.messages, CopilotMessage(role: 'assistant', content: t)],
    );
  }

  String _clarifyingQuestionForReason(String reasonCode, String language) {
    final ru = language.toUpperCase() == 'RU';
    final uz = language.toUpperCase() == 'UZ';
    switch (reasonCode) {
      case 'DOCTOR_MISSING':
      case 'DOCTOR_NOT_IN_SUGGESTED_LIST':
      case 'DOCTOR_NOT_FOUND':
      case 'NO_SUGGESTED_DOCTORS':
        if (ru) return 'Уточните, пожалуйста, какого врача из списка вы выбираете?';
        if (uz) return "Iltimos, ro'yxatdan qaysi shifokorni tanlayotganingizni aniqlashtiring.";
        return 'Please confirm which doctor from the suggested list you want to book with.';
      case 'TIME_MISSING':
      case 'INVALID_PREFERRED_TIME':
      case 'PREFERRED_TIME_IN_PAST':
        if (ru) return 'Укажите, пожалуйста, удобные будущие дату и время для записи.';
        if (uz) return 'Iltimos, qabul uchun kelajakdagi sana va vaqtni ayting.';
        return 'Please share a future preferred date and time for the appointment.';
      case 'VISIT_TYPE_MISSING':
        if (ru) return 'Вы предпочитаете видео-консультацию или очный визит в клинику?';
        if (uz) return "Video qabulni xohlaysizmi yoki klinikaga borib ko'rinishni?";
        return 'Do you prefer a video consultation or an in-person clinic visit?';
      default:
        if (ru) return 'Чтобы продолжить запись, уточните врача, дату/время и формат визита.';
        if (uz) return 'Band qilishni davom ettirish uchun shifokor, sana/vaqt va qabul turini aniqlashtiring.';
        return 'To continue booking, please confirm doctor, preferred date/time, and visit type.';
    }
  }

  void _handleResolveFailure(Map<String, dynamic> res, String language) {
    final code = (res['reasonCode'] as String?)?.trim() ?? '';
    final backendMessage = (res['message'] as String?)?.trim();
    if (backendMessage != null && backendMessage.isNotEmpty) {
      state = state.copyWith(lastError: backendMessage);
    }
    _appendAssistantIfNew(_clarifyingQuestionForReason(code, language));
    state = state.copyWith(
      memoryBookingDecisions: [...state.memoryBookingDecisions, 'booking_failed:$code'],
    );
    _persistMemory();
  }

  /// Resolves [lastSuggestedDoctorIds] for the booking API: omit when never loaded; pass empty list when search had no hits.
  List<int>? _allowedDoctorIdsPayload() {
    final raw = state.lastSuggestedDoctorIds;
    if (raw == null) return null;
    final ids = raw.map((id) => int.tryParse(id)).whereType<int>().toList();
    return ids;
  }

  /// [language] e.g. EN, RU, UZ. Returns [previewMessage] when the server needs in-app confirmation before booking.
  Future<String?> sendUserMessage(String text, String language) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty || state.isSending) return null;

    final userMsg = CopilotMessage(role: 'user', content: trimmed);
    state = state.copyWith(
      messages: [...state.messages, userMsg],
      clearStreaming: true,
      clearError: true,
      isSending: true,
      isThinking: true,
    );

    final payload = state.messages
        .map((m) => {'role': m.role, 'content': m.content})
        .toList();

    var assistantBuffer = '';

    try {
      await for (final event in _api.streamChat(messages: payload, language: language)) {
        if (event is CopilotTokenEvent) {
          assistantBuffer += event.token;
          state = state.copyWith(streamingText: assistantBuffer);
        }
      }

      state = state.copyWith(
        messages: [
          ...state.messages,
          CopilotMessage(role: 'assistant', content: assistantBuffer.trim()),
        ],
        clearStreaming: true,
        isSending: false,
        isThinking: false,
        contextSummary: _rebuildSummary([
          ...state.messages,
          CopilotMessage(role: 'assistant', content: assistantBuffer.trim()),
        ]),
        memorySymptoms: {
          ...state.memorySymptoms,
          ..._extractSymptomsFromText(trimmed),
        }.toList(),
      );
      await _persistMemory();
      return null;
    } on CopilotStreamException catch (e) {
      state = state.copyWith(
        isSending: false,
        isThinking: false,
        clearStreaming: true,
        lastError: e.message,
      );
      return null;
    } catch (e) {
      state = state.copyWith(
        isSending: false,
        isThinking: false,
        clearStreaming: true,
        lastError: e.toString(),
      );
      return null;
    }
  }

  void clearError() {
    state = state.copyWith(clearError: true);
  }

  void appendAssistantMessage(String content) {
    _appendAssistantIfNew(content);
    _persistMemory();
  }

  /// Second step after the user confirms the booking preview in the UI.
  Future<void> confirmBookingFromChat(String language) async {
    if (state.messages.isEmpty) return;
    try {
      final allowed = _allowedDoctorIdsPayload();
      final payload = state.messages
          .map((m) => {'role': m.role, 'content': m.content})
          .toList();
      final res = await _api.resolveBookingFromChat(
        messages: payload,
        language: language,
        allowedDoctorIds: allowed,
        confirmAutoBook: true,
      );
      if (res['booked'] == true) {
        _ref.invalidate(bookingsProvider);
        final msg = res['followUpMessage'] as String? ?? '';
        if (msg.isNotEmpty) {
          state = state.copyWith(
            messages: [...state.messages, CopilotMessage(role: 'assistant', content: msg)],
            memoryBookingDecisions: [...state.memoryBookingDecisions, 'booked'],
          );
          await _persistMemory();
        }
        return;
      }
      _handleResolveFailure(res, language);
    } catch (e) {
      state = state.copyWith(lastError: e.toString());
    }
  }

  Future<String?> _peekResolveBookingFromChat(String language) async {
    if (state.messages.isEmpty) return null;
    final allowed = _allowedDoctorIdsPayload();
    if (allowed != null && allowed.isEmpty) {
      return null;
    }
    try {
      final payload = state.messages
          .map((m) => {'role': m.role, 'content': m.content})
          .toList();
      final res = await _api.resolveBookingFromChat(
        messages: payload,
        language: language,
        allowedDoctorIds: allowed,
        confirmAutoBook: false,
      );
      if (res['needsClientConfirmation'] == true) {
        final preview = (res['previewMessage'] as String?)?.trim() ?? '';
        if (preview.isNotEmpty) return preview;
      }
      if (res['booked'] != true) {
        _handleResolveFailure(res, language);
      }
    } catch (_) {}
    return null;
  }

  /// Public preview resolver; call this after doctor suggestions are refreshed so allowed doctor IDs are current.
  Future<String?> resolveBookingPreviewFromChat(String language) {
    return _peekResolveBookingFromChat(language);
  }

  Future<List<DoctorModel>> fetchSuggestedDoctors(String symptomsText) async {
    final list = await _api.suggestDoctors(symptomsText);
    state = state.copyWith(
      lastSuggestedDoctorIds: list.map((d) => d.id).toList(),
    );
    return list;
  }

  /// Best-effort suggestion from free-text chat input.
  /// Keeps chat responsive and only surfaces errors in [lastError] when request fails.
  Future<List<DoctorModel>> autoSuggestDoctorsFromChatText(String text) async {
    final query = text.trim();
    if (query.isEmpty) return const [];
    try {
      final list = await _api.suggestDoctors(query);
      state = state.copyWith(
        lastSuggestedDoctorIds: list.map((d) => d.id).toList(),
      );
      return list;
    } catch (e) {
      state = state.copyWith(lastError: e.toString());
      return const [];
    }
  }

  /// Smart suggestion that sends the entire chat history. When the server says we lack enough info,
  /// returns an empty doctor list and surfaces the clarifying question as an assistant chat bubble so the
  /// copilot asks one concrete follow-up instead of showing irrelevant doctors.
  Future<List<DoctorModel>> autoSuggestDoctorsFromChat(String language) async {
    if (state.messages.where((m) => m.role == 'user').isEmpty) return const [];
    state = state.copyWith(isAnalyzingSymptoms: true, isFindingDoctors: true);
    try {
      final payload = state.messages
          .map((m) => {'role': m.role, 'content': m.content})
          .toList();
      final res = await _api.suggestDoctorsFromChat(
        messages: payload,
        language: language,
      );
      if ((res.uncertaintyMessage ?? '').trim().isNotEmpty) {
        _appendAssistantIfNew(res.uncertaintyMessage!.trim());
      }
      if (res.needsMoreInfo) {
        state = state.copyWith(
          lastSuggestedDoctorIds: const [],
        );
        final q = res.clarifyingQuestion;
        if (q != null && q.isNotEmpty) {
          final alreadyAsked = state.messages.isNotEmpty &&
              state.messages.last.role == 'assistant' &&
              state.messages.last.content.trim() == q;
          if (!alreadyAsked) {
            state = state.copyWith(
              messages: [
                ...state.messages,
                CopilotMessage(role: 'assistant', content: q),
              ],
            );
          }
        }
        state = state.copyWith(isAnalyzingSymptoms: false, isFindingDoctors: false);
        await _persistMemory();
        return const [];
      }
      state = state.copyWith(
        lastSuggestedDoctorIds: res.doctors.map((d) => d.id).toList(),
        memorySuspectedConditions: {
          ...state.memorySuspectedConditions,
          ...res.specialties,
        }.toList(),
        isAnalyzingSymptoms: false,
        isFindingDoctors: false,
      );
      await _persistMemory();
      return res.doctors;
    } catch (e) {
      state = state.copyWith(
        lastError: e.toString(),
        isAnalyzingSymptoms: false,
        isFindingDoctors: false,
      );
      return const [];
    }
  }
}

final copilotChatProvider =
    StateNotifierProvider<CopilotChatNotifier, CopilotChatState>((ref) {
  return CopilotChatNotifier(ref);
});
