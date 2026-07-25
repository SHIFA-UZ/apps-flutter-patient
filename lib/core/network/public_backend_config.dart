import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shifa_patient_app_v1/core/network/api_client.dart';

/// Cached values from GET /api/public/config (no auth).
class PublicBackendConfig {
  PublicBackendConfig._();

  static Map<String, dynamic>? _cache;
  static bool _fetching = false;

  /// Clears memoized config (e.g. after environment switch during tests).
  static void clearCache() {
    _cache = null;
  }

  static Future<Map<String, dynamic>> _ensureLoaded() async {
    final hit = _cache;
    if (hit != null) return hit;

    if (_fetching) {
      for (var i = 0; i < 20; i++) {
        await Future<void>.delayed(const Duration(milliseconds: 80));
        if (_cache != null) return _cache!;
      }
    }

    try {
      _fetching = true;
      // ApiClient.apiBaseUrl already ends with /api and falls back in release.
      final uri = Uri.parse('${ApiClient.apiBaseUrl}/public/config');
      final response =
          await http.get(uri).timeout(const Duration(seconds: 5));
      if (response.statusCode == 200) {
        final body = json.decode(response.body);
        if (body is Map<String, dynamic>) {
          _cache = body;
          return body;
        }
      }
    } catch (_) {
      // ignore network errors
    } finally {
      _fetching = false;
    }
    _cache ??= {};
    return _cache!;
  }

  /// When backend sets `transcriptionFeedbackEnabled=true`, clients show "Report incorrect text".
  static Future<bool> transcriptionFeedbackEnabled() async {
    final map = await _ensureLoaded();
    final raw = map['transcriptionFeedbackEnabled'];
    return raw == true || raw == 'true';
  }
}
