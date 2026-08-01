import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Secure storage for auth (JWT, user_id). Excluded from backup so sessions are not restored onto new devices.
class StorageService {
  static const String authTokenKey = 'auth_token';
  static const String authTokenSavedAtKey = 'auth_token_saved_at';
  static const String userIdKey = 'user_id';

  /// Dedicated secure storage for auth. Uses Android Keystore / iOS Keychain.
  static final FlutterSecureStorage _authStorage = FlutterSecureStorage(
    aOptions: const AndroidOptions(
      sharedPreferencesName: 'shifa_auth_secure',
      // An undecryptable entry (restored backup, rotated Keystore key) would
      // otherwise throw on every read and lock the user out permanently.
      // Clearing it costs a re-login instead.
      resetOnError: true,
    ),
  );

  Future<void> saveAuthToken(String token) async {
    await _authStorage.write(key: authTokenKey, value: token);
    // The timestamp only feeds the 401 grace period, so losing it must not
    // fail a sign-in that already produced a valid token.
    try {
      await _authStorage.write(
        key: authTokenSavedAtKey,
        value: DateTime.now().toIso8601String(),
      );
    } catch (_) {}
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
