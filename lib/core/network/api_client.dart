import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:shifa_patient_app_v1/core/config/app_config.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/core/utils/storage_service.dart';

class ApiClient {
  late final Dio _dio;
  VoidCallback? _onAuthError;
  bool _isLoggingOut = false;
  DateTime? _lastLogoutAttempt;

  static String get apiBaseUrl {
    // If API_BASE_URL is provided via --dart-define, use it
    if (AppConfig.apiBaseUrl.isNotEmpty) {
      // Remove /api suffix if present, as we add it below
      final base = AppConfig.apiBaseUrl.replaceAll(RegExp(r'/api/?$'), '');
      final url = '$base/api';
      AppLogger.debug('🔗 Using API_BASE_URL from environment: $url');
      return url;
    }

    // Release build without API_BASE_URL: use production URL so Play Store testers can reach backend
    if (kReleaseMode) {
      final base =
          AppConfig.productionApiBaseUrl.replaceAll(RegExp(r'/api/?$'), '');
      final url = '$base/api';
      AppLogger.debug('🔗 Release build using production API: $url');
      return url;
    }

    // Debug/development: localhost (emulator uses 10.0.2.2 on Android)
    final defaultUrl = kIsWeb
        ? 'https://shifa-doc-backend-mvp-production.up.railway.app/api'
        : (defaultTargetPlatform == TargetPlatform.android
            ? 'https://shifa-doc-backend-mvp-production.up.railway.app/api'
            : 'https://shifa-doc-backend-mvp-production.up.railway.app/api');
    AppLogger.debug('⚠️ API_BASE_URL not set, using default: $defaultUrl');
    return defaultUrl;
  }

  static String get publicBaseUrl {
    if (kIsWeb) return 'http://localhost:8090';
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return 'http://10.0.2.2:8090';
      default:
        return 'http://localhost:8090';
    }
  }

  /// Auth paths that must work without a JWT. A slow or broken Android Keystore
  /// must never delay or block sign-in, registration or OTP delivery.
  static bool isUnauthenticatedAuthPath(String path) {
    final normalized =
        path.contains('://') ? Uri.tryParse(path)?.path ?? path : path;
    final p = normalized.startsWith('/') ? normalized : '/$normalized';
    const publicSuffixes = <String>[
      '/auth/login',
      '/auth/register',
      '/auth/register-patient',
      '/auth/register-clinic-staff',
      '/auth/verify-key',
      '/auth/check-existing-patient',
      '/auth/check-existing-doctor',
      '/auth/check-identifier',
      '/auth/create-patient-for-doctor',
      '/auth/send-email-otp',
      '/auth/send-sms-otp',
      '/auth/verify',
      '/auth/verify-email-otp',
      '/auth/send-login-otp',
      '/auth/send-forgot-password-otp',
      '/auth/forgot-password-reset',
    ];
    return publicSuffixes.any((s) => p == s || p.endsWith(s));
  }

  ApiClient() {
    AppLogger.debug('🔗 ApiClient initialized with baseUrl: $apiBaseUrl');
    
    _dio = Dio(
      BaseOptions(
        baseUrl: apiBaseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(seconds: 30),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Unauthenticated auth calls carry no token, so never touch the
          // Keystore for them: sign-in, registration and OTP delivery must not
          // depend on it being readable.
          if (!isUnauthenticatedAuthPath(options.path)) {
            try {
              final token = await StorageService().getAuthToken();
              if (token != null) {
                options.headers['Authorization'] = 'Bearer $token';
              }
            } catch (_) {
              // Proceed unauthenticated and let the response decide, rather
              // than failing the request outright.
            }
          }
          return handler.next(options);
        },
        onError: (error, handler) async {
          if (error.response != null) {
            AppLogger.apiError(
              error.response?.statusCode,
              error.response?.data,
              error.requestOptions.path,
            );
            
            // Only 401 = token invalid/expired → logout. 403 = valid token but forbidden (e.g. wrong role) → do not logout.
            final statusCode = error.response?.statusCode;
            if (statusCode == 401) {
              final path = error.requestOptions.path;
              final isPublicEndpoint = path.startsWith('/public/') ||
                  path.startsWith('/auth/');

              if (!isPublicEndpoint && !_isLoggingOut) {
                // The request went out with no Bearer token (Keystore read
                // timed out above), so this 401 says nothing about whether the
                // stored session is still valid. Never sign the user out here.
                if (!error.requestOptions.headers
                    .containsKey('Authorization')) {
                  return handler.next(error);
                }

                final now = DateTime.now();
                if (_lastLogoutAttempt != null) {
                  final timeSinceLastAttempt = now.difference(_lastLogoutAttempt!);
                  if (timeSinceLastAttempt.inSeconds < 5) {
                    return handler.next(error);
                  }
                }

                final storage = StorageService();
                String? token;
                try {
                  token = await storage.getAuthToken();
                } catch (_) {
                  // Cannot confirm the session is stale — keep the user in.
                  return handler.next(error);
                }
                if (token == null) {
                  return handler.next(error);
                }

                // Grace period: do not logout within 10s of saving token (avoids race after login)
                const gracePeriodSeconds = 10;
                String? savedAtStr;
                try {
                  savedAtStr = await storage.getAuthTokenSavedAt();
                } catch (_) {
                  savedAtStr = null;
                }
                if (savedAtStr != null) {
                  try {
                    final savedAt = DateTime.parse(savedAtStr);
                    if (now.difference(savedAt).inSeconds < gracePeriodSeconds) {
                      return handler.next(error);
                    }
                  } catch (_) {}
                }

                _isLoggingOut = true;
                _lastLogoutAttempt = now;

                await storage.clearAuthToken();

                Future.microtask(() {
                  _onAuthError?.call();
                });

                Future.delayed(const Duration(seconds: 5), () {
                  _isLoggingOut = false;
                });
              }
            }
          } else {
            AppLogger.error('API Error (no response):', error.message);
          }
          return handler.next(error);
        },
      ),
    );
  }

  /// Set callback to be called when authentication error (401) is detected
  void setOnAuthError(VoidCallback? callback) {
    _onAuthError = callback;
  }

  /// Base URL without /api suffix (e.g. for multipart uploads that build their own path).
  String get baseUrl {
    final url = apiBaseUrl;
    return url.replaceAll(RegExp(r'/api/?$'), '');
  }

  /// Get auth token from secure storage. Used by services that bypass Dio (e.g. multipart upload).
  Future<String?> getAuthToken() async {
    return await StorageService().getAuthToken();
  }

  Dio get dio => _dio;

  Future<Response> get(String path, {Map<String, dynamic>? queryParameters}) {
    return _dio.get(path, queryParameters: queryParameters);
  }

  Future<Response> post(String path, {dynamic data, Map<String, dynamic>? queryParameters}) {
    return _dio.post(path, data: data, queryParameters: queryParameters);
  }

  Future<Response> put(String path, {dynamic data}) {
    return _dio.put(path, data: data);
  }

  Future<Response> patch(String path, {dynamic data}) {
    return _dio.patch(path, data: data);
  }

  Future<Response> delete(String path) {
    return _dio.delete(path);
  }

  Future<Response> uploadFile(
    String path,
    String filePath, {
    String fieldName = 'file',
    Map<String, dynamic>? additionalFields,
  }) async {
    final formData = FormData.fromMap({
      fieldName: await MultipartFile.fromFile(filePath),
      ...?additionalFields,
    });
    return dio.post(path, data: formData);
  }
}
