import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

class CopilotMemoryStore {
  static const _messagesKey = 'copilot_messages_v1';
  static const _summaryKey = 'copilot_summary_v1';
  static const _stateKey = 'copilot_structured_state_v1';

  Future<void> save({
    required List<Map<String, String>> messages,
    required String summary,
    required Map<String, dynamic> structuredState,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_messagesKey, jsonEncode(messages));
    await prefs.setString(_summaryKey, summary);
    await prefs.setString(_stateKey, jsonEncode(structuredState));
  }

  Future<({List<Map<String, String>> messages, String summary, Map<String, dynamic> structuredState})> load() async {
    final prefs = await SharedPreferences.getInstance();
    final rawMessages = prefs.getString(_messagesKey);
    final summary = prefs.getString(_summaryKey) ?? '';
    final rawState = prefs.getString(_stateKey);
    final state = rawState == null ? <String, dynamic>{} : (jsonDecode(rawState) as Map<String, dynamic>);
    if (rawMessages == null || rawMessages.isEmpty) {
      return (messages: const <Map<String, String>>[], summary: summary, structuredState: state);
    }
    try {
      final decoded = jsonDecode(rawMessages) as List<dynamic>;
      final msgs = decoded
          .map((e) => Map<String, String>.from(e as Map))
          .where((m) => (m['role'] ?? '').isNotEmpty && (m['content'] ?? '').trim().isNotEmpty)
          .toList();
      return (messages: msgs, summary: summary, structuredState: state);
    } catch (_) {
      return (messages: const <Map<String, String>>[], summary: summary, structuredState: state);
    }
  }
}

