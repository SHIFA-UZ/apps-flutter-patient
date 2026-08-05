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
  /// Set via: flutter run --dart-define=ENVIRONMENT=production
  static const String environment = String.fromEnvironment(
    'ENVIRONMENT',
    defaultValue: 'development',
  );

  /// Whether the app is running in production mode
  static bool get isProduction => environment == 'production';

  /// Whether the app is running in development mode
  static bool get isDevelopment => environment == 'development';

  /// Whether the app is running in staging mode
  static bool get isStaging => environment == 'staging';

  /// Enable debug logging (disabled in production)
  static bool get enableDebugLogging => !isProduction;

  /// Display name shown in version footers.
  static const String appDisplayName = 'Shifa Bemor';

  /// Human-readable release date for the current [pubspec] versionName.
  /// Update this whenever you bump the version shipped to stores.
  static const String releaseDateLabel = '5 Aug 2026';

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
    return 'Environment: $environment | API: ${apiBaseUrl.isEmpty ? "default" : apiBaseUrl}';
  }
}
