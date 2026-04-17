import 'package:shared_preferences/shared_preferences.dart';

/// Tracks failed login attempts and enforces a temporary lockout after max attempts.
/// Persists state so limits apply across app restarts.
class LoginAttemptService {
  LoginAttemptService();

  static const int maxAttempts = 5;
  static const int lockoutDurationMinutes = 5;

  static const String _keyFailedAttempts = 'login_failed_attempts';
  static const String _keyLockoutUntilEpochMs = 'login_lockout_until_epoch_ms';

  Future<SharedPreferences> _prefs() => SharedPreferences.getInstance();

  /// Number of failed attempts in the current window (resets when lockout expires).
  Future<int> getFailedAttempts() async {
    final prefs = await _prefs();
    if (await _isLockedOut(prefs)) {
      return maxAttempts;
    }
    return prefs.getInt(_keyFailedAttempts) ?? 0;
  }

  /// Epoch milliseconds when lockout ends; null if not locked.
  Future<int?> getLockoutUntilEpochMs() async {
    final prefs = await _prefs();
    final until = prefs.getInt(_keyLockoutUntilEpochMs);
    if (until == null) return null;
    if (DateTime.now().millisecondsSinceEpoch >= until) {
      await _clearLockout(prefs);
      return null;
    }
    return until;
  }

  Future<bool> _isLockedOut(SharedPreferences prefs) async {
    final until = prefs.getInt(_keyLockoutUntilEpochMs);
    if (until == null) return false;
    if (DateTime.now().millisecondsSinceEpoch >= until) {
      await _clearLockout(prefs);
      return false;
    }
    return true;
  }

  Future<void> _clearLockout(SharedPreferences prefs) async {
    await prefs.remove(_keyLockoutUntilEpochMs);
    await prefs.remove(_keyFailedAttempts);
  }

  /// True if user is currently locked out.
  Future<bool> isLockedOut() async {
    final prefs = await _prefs();
    return _isLockedOut(prefs);
  }

  /// How many seconds until lockout ends; 0 if not locked.
  Future<int> lockoutRemainingSeconds() async {
    final until = await getLockoutUntilEpochMs();
    if (until == null) return 0;
    final remaining = (until - DateTime.now().millisecondsSinceEpoch) ~/ 1000;
    return remaining > 0 ? remaining : 0;
  }

  /// Attempts remaining before lockout (0 when locked).
  Future<int> remainingAttempts() async {
    if (await isLockedOut()) return 0;
    final n = await getFailedAttempts();
    return (maxAttempts - n).clamp(0, maxAttempts);
  }

  /// Call after a failed login. Returns true if this was the 5th attempt (just locked).
  Future<bool> recordFailedAttempt() async {
    final prefs = await _prefs();
    if (await _isLockedOut(prefs)) return true;

    final current = (prefs.getInt(_keyFailedAttempts) ?? 0) + 1;
    await prefs.setInt(_keyFailedAttempts, current);

    if (current >= maxAttempts) {
      final until = DateTime.now().add(const Duration(minutes: lockoutDurationMinutes));
      await prefs.setInt(_keyLockoutUntilEpochMs, until.millisecondsSinceEpoch);
      return true;
    }
    return false;
  }

  /// Call after a successful login.
  Future<void> clearAttempts() async {
    final prefs = await _prefs();
    await _clearLockout(prefs);
  }
}
