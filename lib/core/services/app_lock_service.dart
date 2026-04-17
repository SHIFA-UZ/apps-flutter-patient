import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';

class AppLockService {
  /// Use explicit Android options to avoid PIN verification failures on some devices.
  /// - sharedPreferencesName: dedicated storage for app lock
  /// - resetOnError: false to prevent PIN loss on transient decryption errors (e.g. BadPaddingException)
  static final _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(
      sharedPreferencesName: 'shifa_app_lock_prefs',
      resetOnError: false,
    ),
  );
  static const _pinKey = 'app_lock_pin';
  static const _biometricEnabledKey = 'biometric_enabled';
  static const _lockEnabledKey = 'app_lock_enabled';
  static const _inactivitySecondsKey = 'app_lock_inactivity_seconds';
  static const _hasSeenBiometricPromptKey = 'app_lock_seen_biometric_prompt';
  
  /// Max PIN attempts before force logout
  static const int maxPinAttempts = 5;

  /// PIN must be 4-6 digits (numeric only)
  static const int minPinLength = 4;
  static const int maxPinLength = 6;

  /// Cooldown seconds after failed attempt. No cooldown for 1st–3rd; 30s from 4th onward.
  static int cooldownSecondsAfterFailedAttempt(int attemptNumber) {
    if (attemptNumber <= 3) return 0;
    return 30;
  }

  /// Validate PIN: 4-6 digits only
  static bool isValidPin(String pin) {
    final trimmed = pin.trim();
    if (trimmed.length < minPinLength || trimmed.length > maxPinLength) return false;
    return RegExp(r'^\d+$').hasMatch(trimmed);
  }
  
  /// Default inactivity threshold (seconds) before lock
  static const int defaultInactivitySeconds = 30;
  
  final LocalAuthentication _localAuth = LocalAuthentication();

  /// Check if app lock is enabled
  Future<bool> isLockEnabled() async {
    final enabled = await _storage.read(key: _lockEnabledKey);
    return enabled == 'true';
  }

  /// Enable or disable app lock
  Future<void> setLockEnabled(bool enabled) async {
    await _storage.write(key: _lockEnabledKey, value: enabled.toString());
  }

  /// Check if biometric authentication is available
  Future<bool> isBiometricAvailable() async {
    try {
      final isAvailable = await _localAuth.canCheckBiometrics;
      final isDeviceSupported = await _localAuth.isDeviceSupported();
      return isAvailable && isDeviceSupported;
    } catch (e) {
      return false;
    }
  }

  /// Get available biometric types
  Future<List<BiometricType>> getAvailableBiometrics() async {
    try {
      return await _localAuth.getAvailableBiometrics();
    } catch (e) {
      return [];
    }
  }

  /// Check if biometric is enabled
  Future<bool> isBiometricEnabled() async {
    final enabled = await _storage.read(key: _biometricEnabledKey);
    return enabled == 'true';
  }

  /// Enable or disable biometric authentication
  Future<void> setBiometricEnabled(bool enabled) async {
    await _storage.write(key: _biometricEnabledKey, value: enabled.toString());
  }

  /// Set PIN code (must be 4-6 digits)
  Future<void> setPin(String pin) async {
    if (!isValidPin(pin)) {
      throw ArgumentError('PIN must be 4-6 digits');
    }
    await _storage.write(key: _pinKey, value: pin.trim());
  }

  /// Get stored PIN (for verification only)
  Future<String?> getPin() async {
    return await _storage.read(key: _pinKey);
  }

  /// Authenticate with biometric
  Future<bool> authenticateWithBiometric() async {
    try {
      final isAvailable = await isBiometricAvailable();
      if (!isAvailable) return false;

      final isEnabled = await isBiometricEnabled();
      if (!isEnabled) return false;

      final authenticated = await _localAuth.authenticate(
        localizedReason: 'Please authenticate to unlock the app',
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
        ),
      );

      return authenticated;
    } catch (e) {
      return false;
    }
  }

  /// Verify PIN code (enteredPin must be 4-6 digits)
  Future<bool> verifyPin(String enteredPin) async {
    if (!isValidPin(enteredPin)) return false;
    final storedPin = await getPin();
    if (storedPin == null || storedPin.isEmpty) return false;
    return storedPin.trim() == enteredPin.trim();
  }

  /// Check if PIN is set
  Future<bool> hasPin() async {
    final pin = await getPin();
    return pin != null && pin.isNotEmpty;
  }

  /// Clear PIN (for logout or reset)
  Future<void> clearPin() async {
    await _storage.delete(key: _pinKey);
  }

  /// Get inactivity threshold in seconds (default 30)
  Future<int> getInactivitySeconds() async {
    final v = await _storage.read(key: _inactivitySecondsKey);
    if (v == null) return defaultInactivitySeconds;
    return int.tryParse(v) ?? defaultInactivitySeconds;
  }

  /// Set inactivity threshold in seconds
  Future<void> setInactivitySeconds(int seconds) async {
    await _storage.write(key: _inactivitySecondsKey, value: seconds.toString());
  }

  /// Check if user has seen post-login biometric prompt
  Future<bool> hasSeenBiometricPrompt() async {
    final v = await _storage.read(key: _hasSeenBiometricPromptKey);
    return v == 'true';
  }

  /// Mark that user has seen post-login biometric prompt
  Future<void> setHasSeenBiometricPrompt() async {
    await _storage.write(key: _hasSeenBiometricPromptKey, value: 'true');
  }
}
