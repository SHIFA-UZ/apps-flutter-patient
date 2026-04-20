import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';
import 'package:shifa_patient_app_v1/features/copilot/data/copilot_api.dart';
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
  final String? lastError;
  /// `null` = doctor suggestions not loaded for this session; non-empty = suggested ids; empty = search returned no doctors.
  final List<String>? lastSuggestedDoctorIds;

  CopilotChatState({
    this.messages = const [],
    this.streamingText,
    this.isSending = false,
    this.lastError,
    this.lastSuggestedDoctorIds,
  });

  CopilotChatState copyWith({
    List<CopilotMessage>? messages,
    String? streamingText,
    bool? isSending,
    String? lastError,
    List<String>? lastSuggestedDoctorIds,
    bool clearStreaming = false,
    bool clearError = false,
  }) {
    return CopilotChatState(
      messages: messages ?? this.messages,
      streamingText: clearStreaming ? null : (streamingText ?? this.streamingText),
      isSending: isSending ?? this.isSending,
      lastError: clearError ? null : (lastError ?? this.lastError),
      lastSuggestedDoctorIds: lastSuggestedDoctorIds ?? this.lastSuggestedDoctorIds,
    );
  }
}

class CopilotChatNotifier extends StateNotifier<CopilotChatState> {
  CopilotChatNotifier(this._ref) : super(CopilotChatState());

  final Ref _ref;

  CopilotApi get _api => _ref.read(copilotApiProvider);

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
      );
      return await _peekResolveBookingFromChat(language);
    } on CopilotStreamException catch (e) {
      state = state.copyWith(
        isSending: false,
        clearStreaming: true,
        lastError: e.message,
      );
      return null;
    } catch (e) {
      state = state.copyWith(
        isSending: false,
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
    final t = content.trim();
    if (t.isEmpty) return;
    state = state.copyWith(
      messages: [...state.messages, CopilotMessage(role: 'assistant', content: t)],
    );
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
          );
        }
        return;
      }
      final failure = (res['message'] as String?)?.trim();
      if (failure != null && failure.isNotEmpty) {
        state = state.copyWith(lastError: failure);
      }
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
    } catch (_) {}
    return null;
  }

  Future<List<DoctorModel>> fetchSuggestedDoctors(String symptomsText) async {
    final list = await _api.suggestDoctors(symptomsText);
    state = state.copyWith(
      lastSuggestedDoctorIds: list.map((d) => d.id).toList(),
    );
    return list;
  }
}

final copilotChatProvider =
    StateNotifierProvider<CopilotChatNotifier, CopilotChatState>((ref) {
  return CopilotChatNotifier(ref);
});
