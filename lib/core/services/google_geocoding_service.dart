import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shifa_patient_app_v1/core/config/app_config.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';

/// Service for Google Maps Geocoding API
///
/// API key can be set:
/// 1. At build time: --dart-define=GOOGLE_MAPS_API_KEY=your_key_here
/// 2. Via backend: GOOGLE_MAPS_API_KEY on server, fetched from /api/public/config
class GoogleGeocodingService {
  static String? _cachedApiKey;
  static bool _isFetching = false;

  static void _log(String message) {
    if (kDebugMode) debugPrint(message);
  }

  /// Get API key - tries build-time first, then fetches from backend if needed
  static Future<String> _getApiKey() async {
    final buildTimeKey = AppConfig.googleMapsApiKey;
    if (buildTimeKey.isNotEmpty) {
      return buildTimeKey;
    }

    if (_cachedApiKey != null && _cachedApiKey!.isNotEmpty) {
      return _cachedApiKey!;
    }

    if (_isFetching) return '';

    try {
      _isFetching = true;
      final configUrl = Uri.parse('${ApiClient.apiBaseUrl}/public/config');
      final response = await http.get(configUrl).timeout(
        const Duration(seconds: 5),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final key = data['googleMapsApiKey'] as String?;
        if (key != null && key.isNotEmpty) {
          _cachedApiKey = key;
          _log('Google Maps API key fetched from backend');
          return key;
        }
      }
    } catch (e) {
      _log('Failed to fetch Google Maps API key from backend: $e');
    } finally {
      _isFetching = false;
    }

    return '';
  }

  static const String _baseUrl = 'https://maps.googleapis.com/maps/api/geocode/json';

  /// Reverse geocoding: Convert coordinates to address
  static Future<Map<String, dynamic>?> reverseGeocode(
    double latitude,
    double longitude,
  ) async {
    try {
      final apiKey = await _getApiKey();
      if (apiKey.isEmpty) {
        _log('WARNING: Google Maps API key not set. Geocoding will fail.');
        return null;
      }

      final url = Uri.parse(
        '$_baseUrl?latlng=$latitude,$longitude&key=$apiKey&language=en',
      );

      final response = await http.get(url);
      // Never log the full URL — it contains the API key.
      _log('Google Geocoding reverse status: ${response.statusCode}');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        if (data['status'] == 'OK' &&
            data['results'] != null &&
            data['results'].isNotEmpty) {
          return _parseAddress(data['results'][0]);
        }
        _log('Google Geocoding API error: ${data['status']}');
        return null;
      }
      return null;
    } catch (e) {
      _log('Reverse geocoding error: $e');
      return null;
    }
  }

  /// Forward geocoding: Convert address to coordinates
  static Future<Map<String, dynamic>?> geocode(String address) async {
    try {
      final apiKey = await _getApiKey();
      if (apiKey.isEmpty) {
        _log('WARNING: Google Maps API key not set. Geocoding will fail.');
        return null;
      }

      final encodedAddress = Uri.encodeComponent(address);
      final url = Uri.parse(
        '$_baseUrl?address=$encodedAddress&key=$apiKey&language=en',
      );

      final response = await http.get(url);
      _log('Google Geocoding forward status: ${response.statusCode}');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        if (data['status'] == 'OK' &&
            data['results'] != null &&
            data['results'].isNotEmpty) {
          final result = data['results'][0];
          final location = result['geometry']['location'];

          return {
            'latitude': location['lat'] as double,
            'longitude': location['lng'] as double,
            'formattedAddress': result['formatted_address'] as String?,
            'addressComponents': result['address_components'] as List?,
          };
        }
        _log('Google Geocoding API error: ${data['status']}');
        return null;
      }
      return null;
    } catch (e) {
      _log('Geocoding error: $e');
      return null;
    }
  }

  /// Parse Google Geocoding API response into a structured address
  static Map<String, dynamic> _parseAddress(Map<String, dynamic> result) {
    final addressComponents =
        result['address_components'] as List<dynamic>? ?? [];
    final formattedAddress = result['formatted_address'] as String? ?? '';

    String? streetNumber;
    String? route;
    String? sublocality;
    String? locality;
    String? administrativeAreaLevel1;
    String? administrativeAreaLevel2;
    String? country;
    String? postalCode;

    for (var component in addressComponents) {
      final types = component['types'] as List<dynamic>? ?? [];
      final longName = component['long_name'] as String?;

      if (types.contains('street_number')) {
        streetNumber = longName;
      } else if (types.contains('route')) {
        route = longName;
      } else if (types.contains('sublocality') ||
          types.contains('sublocality_level_1')) {
        sublocality = longName;
      } else if (types.contains('locality')) {
        locality = longName;
      } else if (types.contains('administrative_area_level_1')) {
        administrativeAreaLevel1 = longName;
      } else if (types.contains('administrative_area_level_2')) {
        administrativeAreaLevel2 = longName;
      } else if (types.contains('country')) {
        country = longName;
      } else if (types.contains('postal_code')) {
        postalCode = longName;
      }
    }

    final streetAddressParts = <String>[];
    if (streetNumber != null && streetNumber.isNotEmpty) {
      streetAddressParts.add(streetNumber);
    }
    if (route != null && route.isNotEmpty) {
      streetAddressParts.add(route);
    }
    final streetAddress = streetAddressParts.join(' ');

    return {
      'country': country ?? '',
      'region': administrativeAreaLevel1 ?? '',
      'district': sublocality ?? '',
      'city': locality ?? '',
      'postalCode': postalCode ?? '',
      'streetAddress': streetAddress.isNotEmpty ? streetAddress : route ?? '',
      'formattedAddress': formattedAddress,
      'streetNumber': streetNumber,
      'route': route,
      'sublocality': sublocality,
      'locality': locality,
      'administrativeAreaLevel1': administrativeAreaLevel1,
      'administrativeAreaLevel2': administrativeAreaLevel2,
    };
  }
}
