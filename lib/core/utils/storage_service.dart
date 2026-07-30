import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Secure storage for auth (JWT, user_id). Excluded from backup so sessions are not restored onto new devices.
class StorageService {
  static const String authTokenKey = 'auth_token';
  static const String authTokenSavedAtKey = 'auth_token_saved_at';
  static const String userIdKey = 'user_id';

  /// Dedicated secure storage for auth. Uses Android Keystore / iOS Keychain.
  /// resetOnError: true — if the Keystore key is corrupted (Play update /
  /// reinstall / backup restore), wipe and start fresh instead of throwing
  /// and blocking login with "Something went wrong".
  static final FlutterSecureStorage _authStorage = FlutterSecureStorage(
    aOptions: const AndroidOptions(
      sharedPreferencesName: 'shifa_auth_secure',
      resetOnError: true,
    ),
  );

  Future<void> saveAuthToken(String token) async {
    await _authStorage.write(key: authTokenKey, value: token);
    try {
      await _authStorage.write(
        key: authTokenSavedAtKey,
        value: DateTime.now().toIso8601String(),
      );
    } catch (_) {
      // Non-critical: grace period simply won't apply without a timestamp.
    }
  }

  Future<String?> getAuthToken() async {
    return await _authStorage.read(key: authTokenKey);
  }

  /// For 401 grace-period check in ApiClient. Returns ISO8601 string or null.
  Future<String?> getAuthTokenSavedAt() async {
    return await _authStorage.read(key: authTokenSavedAtKey);
  }

  Future<void> clearAuthToken() async {
    await _authStorage.delete(key: authTokenKey);
    await _authStorage.delete(key: authTokenSavedAtKey);
    await _authStorage.delete(key: userIdKey);
  }

  Future<void> saveUserId(String userId) async {
    await _authStorage.write(key: userIdKey, value: userId);
  }

  Future<String?> getUserId() async {
    return await _authStorage.read(key: userIdKey);
  }

  Future<bool> isAuthenticated() async {
    final token = await getAuthToken();
    return token != null && token.isNotEmpty;
  }
}
