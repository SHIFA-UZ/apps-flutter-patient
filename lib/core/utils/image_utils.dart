import 'package:flutter/foundation.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';

String? normalizePhotoUrl(String? url) {
  if (url == null || url.isEmpty) return null;
  final trimmed = url.trim();
  if (trimmed.isEmpty) return null;
  final isAbsolute = trimmed.startsWith('http://') || trimmed.startsWith('https://');
  
  if (isAbsolute) {
    if (kIsWeb) return trimmed;
    if (defaultTargetPlatform == TargetPlatform.android) {
      final uri = Uri.tryParse(trimmed);
      if (uri != null && (uri.host == 'localhost' || uri.host == '127.0.0.1')) {
        final base = Uri.parse(ApiClient.publicBaseUrl);
        return uri.replace(
          scheme: base.scheme,
          host: base.host,
          port: base.port,
        ).toString();
      }
      return trimmed;
    }
    return trimmed;
  }
  final base = ApiClient.publicBaseUrl;
  final path = trimmed.startsWith('/') ? trimmed.substring(1) : trimmed;
  return '$base/$path';
}

String? withCacheBuster(String? url, int? cacheKey) {
  if (url == null || url.isEmpty) return null;
  
  // Normalize the URL first (especially important for Android)
  final normalized = normalizePhotoUrl(url);
  if (normalized == null) return null;
  
  if (cacheKey == null || cacheKey == 0) return normalized;

  final uri = Uri.parse(normalized);
  final query = Map<String, String>.from(uri.queryParameters);
  query['v'] = cacheKey.toString();
  return uri.replace(queryParameters: query).toString();
}
