import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/utils/storage_service.dart';

/// Thrown when the co-pilot stream returns a structured error (safety, rate limit, etc.).
class CopilotStreamException implements Exception {
  final String code;
  final String message;

  CopilotStreamException(this.code, this.message);

  @override
  String toString() => message;
}

sealed class CopilotStreamEvent {}

class CopilotTokenEvent extends CopilotStreamEvent {
  final String token;
  CopilotTokenEvent(this.token);
}

/// SSE client for [POST /api/patients/me/copilot/stream].
class CopilotApi {
  Future<Map<String, String>> _authHeaders({bool sse = false}) async {
    final token = await StorageService().getAuthToken();
    return {
      if (token != null) 'Authorization': 'Bearer $token',
      'Content-Type': 'application/json',
      if (sse) 'Accept': 'text/event-stream',
    };
  }

  String _extractErrorMessage(http.Response response, String fallback) {
    var message = fallback;
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map<String, dynamic>) {
        message = (decoded['message'] as String?) ??
            (decoded['error'] as String?) ??
            (decoded['detail'] as String?) ??
            fallback;
      }
    } catch (_) {}
    return message;
  }

  /// [language] is EN, RU, or UZ (backend [OutputLanguage] name).
  Stream<CopilotStreamEvent> streamChat({
    required List<Map<String, String>> messages,
    required String language,
  }) async* {
    final uri = Uri.parse('${ApiClient.apiBaseUrl}/patients/me/copilot/stream');
    final request = http.Request('POST', uri);
    request.headers.addAll(await _authHeaders(sse: true));
    request.body = jsonEncode({
      'messages': messages,
      'language': language,
    });

    final response = await request.send();

    if (response.statusCode != 200) {
      final body = await response.stream.bytesToString();
      var code = 'UNKNOWN';
      var message = 'Co-pilot failed: HTTP ${response.statusCode}';
      try {
        final json = jsonDecode(body) as Map<String, dynamic>?;
        if (json != null) {
          code = (json['code'] as String?) ?? code;
          message = (json['message'] as String?) ?? message;
        }
      } catch (_) {}
      throw CopilotStreamException(code, message);
    }

    var buffer = '';
    var currentEvent = '';
    final currentDataLines = <String>[];

    await for (final chunk in response.stream.transform(utf8.decoder)) {
      buffer += chunk;
      while (buffer.contains('\n')) {
        final idx = buffer.indexOf('\n');
        var line = buffer.substring(0, idx);
        buffer = buffer.substring(idx + 1);
        if (line.endsWith('\r')) {
          line = line.substring(0, line.length - 1);
        }

        if (line.isEmpty) {
          final eventName = currentEvent;
          final data = currentDataLines.join('\n');
          currentEvent = '';
          currentDataLines.clear();

          if (eventName == 'error' && data.trim().isNotEmpty) {
            try {
              final json = jsonDecode(data.trimLeft()) as Map<String, dynamic>?;
              final code = (json?['code'] as String?) ?? 'UNKNOWN';
              final msg = (json?['message'] as String?) ?? 'Unknown error';
              throw CopilotStreamException(code, msg);
            } catch (e) {
              if (e is CopilotStreamException) rethrow;
              throw CopilotStreamException('UNKNOWN', data);
            }
          }

          if (data.isNotEmpty) {
            yield CopilotTokenEvent(data);
          }
          continue;
        }

        if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim();
          continue;
        }

        if (line.startsWith('data:')) {
          currentDataLines.add(line.substring(5));
        }
      }
    }

    if (currentDataLines.isNotEmpty) {
      final data = currentDataLines.join('\n');
      if (data.isNotEmpty) {
        yield CopilotTokenEvent(data);
      }
    }
  }

  /// Multipart field name: [file]
  Future<String> transcribeAudio(String filePath) async {
    final uri = Uri.parse('${ApiClient.apiBaseUrl}/patients/me/copilot/transcribe');
    final token = await StorageService().getAuthToken();
    final request = http.MultipartRequest('POST', uri);
    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    request.files.add(await http.MultipartFile.fromPath('file', filePath));
    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    if (response.statusCode != 200) {
      throw Exception('Transcription failed: HTTP ${response.statusCode}');
    }
    final json = jsonDecode(response.body) as Map<String, dynamic>?;
    return (json?['text'] as String?)?.trim() ?? '';
  }

  /// Books the nearest available slot to [preferredStartUtc] on the server (requires consent).
  Future<Map<String, dynamic>> bookCopilotAppointment({
    required String doctorId,
    required DateTime preferredStartUtc,
    required bool isVideo,
    String? reason,
    required bool consentConfirmed,
  }) async {
    final uri = Uri.parse('${ApiClient.apiBaseUrl}/patients/me/copilot/book-appointment');
    final response = await http.post(
      uri,
      headers: await _authHeaders(),
      body: jsonEncode({
        'doctorId': int.parse(doctorId),
        'preferredStartAt': preferredStartUtc.toUtc().toIso8601String(),
        'isVideo': isVideo,
        'reason': reason,
        'consentConfirmed': consentConfirmed,
      }),
    );
    if (response.statusCode != 200) {
      var message = 'Booking failed: HTTP ${response.statusCode}';
      try {
        final j = jsonDecode(response.body);
        if (j is Map<String, dynamic>) {
          message = (j['message'] as String?) ??
              (j['error'] as String?) ??
              (j['detail'] as String?) ??
              message;
        }
      } catch (_) {}
      throw Exception(message);
    }
    return Map<String, dynamic>.from(jsonDecode(response.body) as Map);
  }

  /// LLM-based: infer from chat if patient authorized auto-book; server books if valid.
  Future<Map<String, dynamic>> resolveBookingFromChat({
    required List<Map<String, String>> messages,
    required String language,
    List<int>? allowedDoctorIds,
    bool confirmAutoBook = false,
  }) async {
    final uri = Uri.parse('${ApiClient.apiBaseUrl}/patients/me/copilot/resolve-booking');
    final body = <String, dynamic>{
      'messages': messages,
      'language': language,
      'confirmAutoBook': confirmAutoBook,
      if (allowedDoctorIds != null) 'allowedDoctorIds': allowedDoctorIds,
    };
    final response = await http.post(
      uri,
      headers: await _authHeaders(),
      body: jsonEncode(body),
    );
    if (response.statusCode != 200) {
      throw Exception(
        _extractErrorMessage(response, 'Resolve booking failed: HTTP ${response.statusCode}'),
      );
    }
    return Map<String, dynamic>.from(jsonDecode(response.body) as Map);
  }

  Future<List<DoctorModel>> suggestDoctors(String symptomsText) async {
    final uri = Uri.parse('${ApiClient.apiBaseUrl}/patients/me/copilot/suggest-doctors');
    final response = await http.post(
      uri,
      headers: await _authHeaders(),
      body: jsonEncode({'symptomsText': symptomsText}),
    );
    if (response.statusCode != 200) {
      throw Exception(
        _extractErrorMessage(response, 'Suggest doctors failed: HTTP ${response.statusCode}'),
      );
    }
    final list = jsonDecode(response.body) as List<dynamic>? ?? [];
    return list
        .map((e) => DoctorModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
