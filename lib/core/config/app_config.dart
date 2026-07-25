import 'package:flutter/foundation.dart';

/// Application configuration that reads from environment variables
///
/// This allows the app to work in different environments (dev, staging, production)
/// without code changes. Configuration is set at build time using --dart-define flags.
class AppConfig {
  /// Production backend URL. Used when building release AAB/APK without --dart-define.
  /// Replace with your real backend (e.g. Railway, your domain). No trailing /api.
  /// Must match the domain in android/app/src/main/res/xml/network_security_config.xml
  static const String productionApiBaseUrl =
      'https://shifa-doc-backend-mvp-production.up.railway.app';

  /// Base URL for the API backend
  ///
  /// Set via: flutter build appbundle --dart-define=API_BASE_URL=https://api.yourdomain.com
  /// In release builds, if not set, uses [productionApiBaseUrl].
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: '',
  );

  /// Current environment name (development, staging, production)
  ///
  /// Set via: --dart-define=ENVIRONMENT=production
  static const String environment = String.fromEnvironment(
    'ENVIRONMENT',
    defaultValue: '',
  );

  /// Legacy CI flavor flag (--dart-define=FLAVOR=production).
  static const String flavor = String.fromEnvironment(
    'FLAVOR',
    defaultValue: '',
  );

  /// Whether the app is running in production mode
  static bool get isProduction {
    final env = environment.toLowerCase();
    final flav = flavor.toLowerCase();
    if (env == 'production' || flav == 'production') return true;
    // Store release builds without dart-defines still count as production.
    if (env.isEmpty && flav.isEmpty && kReleaseMode) return true;
    return false;
  }

  /// Whether the app is running in development mode
  static bool get isDevelopment => !isProduction && !isStaging;

  /// Whether the app is running in staging mode
  static bool get isStaging {
    final env = environment.toLowerCase();
    final flav = flavor.toLowerCase();
    return env == 'staging' || flav == 'staging';
  }

  /// Enable debug logging (never in release binaries used for stores)
  static bool get enableDebugLogging => kDebugMode && !isProduction;

  /// Google Maps API key for geocoding services
  ///
  /// Set via: flutter build apk --dart-define=GOOGLE_MAPS_API_KEY=your_key_here
  /// Defaults to empty string (will cause geocoding to fail if not set)
  static const String googleMapsApiKey = String.fromEnvironment(
    'GOOGLE_MAPS_API_KEY',
    defaultValue: '',
  );

  /// Get a human-readable description of the current configuration
  static String get description {
    return 'Environment: ${environment.isEmpty ? (isProduction ? "production" : "development") : environment} | API: ${apiBaseUrl.isEmpty ? "default" : apiBaseUrl}';
  }
}
