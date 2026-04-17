import 'package:flutter/foundation.dart';
import 'package:shifa_patient_app_v1/core/config/app_config.dart';

/// Central logger that only outputs when debug logging is enabled
/// and redacts sensitive / PII-like data. In production builds,
/// logging can be fully disabled via [AppConfig.enableDebugLogging].
class AppLogger {
  AppLogger._();

  static const _sensitiveKeys = {
    'token',
    'authToken',
    'accessToken',
    'refreshToken',
    'password',
    'secret',
    'authorization',
    'cookie',
    'apiKey',
    'api_key',
    // PII-style fields we never want in logs
    'phone',
    'email',
    'name',
    'firstname',
    'lastname',
    'surname',
    'address',
  };

  static bool get _enabled => AppConfig.enableDebugLogging && kDebugMode;

  /// Log a debug message. No-op when debug logging is disabled.
  static void debug(String message, [Object? detail]) {
    if (!_enabled) return;
    if (detail != null) {
      // ignore: avoid_print
      print('$message $detail');
    } else {
      // ignore: avoid_print
      print(message);
    }
  }

  /// Log an error. In production: no-op. In debug: message + optional stack trace.
  static void error(String message, [Object? error, StackTrace? stackTrace]) {
    if (!_enabled) return;
    if (error != null) {
      // ignore: avoid_print
      print('$message $error');
    } else {
      // ignore: avoid_print
      print(message);
    }
    if (stackTrace != null) {
      // ignore: avoid_print
      print(stackTrace);
    }
  }

  /// Log API error. Only enabled when debug logging is on.
  /// Redacts sensitive fields and truncates long bodies.
  static void apiError(int? statusCode, dynamic body, [String? path]) {
    if (!_enabled) return;
    final pathStr = path != null ? ' path=$path' : '';
    final bodyStr = _redact(body);
    final shortBody = bodyStr.length > 300
        ? '${bodyStr.substring(0, 300)}…[truncated]'
        : bodyStr;
    // ignore: avoid_print
    print('API Error: status=$statusCode$pathStr body=$shortBody');
  }

  /// Redact sensitive fields from a value (Map, or toString).
  static String _redact(dynamic value) {
    if (value == null) return 'null';
    if (value is Map) {
      final copy = <String, dynamic>{};
      for (final e in value.entries) {
        final key = e.key.toString().toLowerCase();
        if (_sensitiveKeys.any((k) => key.contains(k))) {
          copy[e.key.toString()] = '[REDACTED]';
        } else {
          copy[e.key.toString()] = _redact(e.value);
        }
      }
      return copy.toString();
    }
    if (value is Iterable && value is! String) {
      return '[${value.length} items]';
    }
    return value.toString();
  }

  /// Build a safe upload/API failure message. In release: status only.
  /// In debug: status + truncated body (for diagnostics only).
  static String safeApiFailureMessage(int statusCode, String body) {
    if (_enabled) {
      final shortBody =
          body.length > 200 ? '${body.substring(0, 200)}…[truncated]' : body;
      return 'Upload failed: $statusCode $shortBody';
    }
    return 'Upload failed: $statusCode';
  }
}
