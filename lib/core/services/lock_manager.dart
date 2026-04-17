import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';

/// Centralized lock lifecycle: only [paused] / [detached] count as background.
/// [inactive] (screenshot, share sheet, camera picker, etc.) is ignored.
/// Lock is required only when app was in background for ≥ [backgroundLockThreshold].
class LockManager extends StateNotifier<bool> with WidgetsBindingObserver {
  LockManager(this._ref) : super(false);

  final Ref _ref;
  DateTime? _backgroundTimestamp;

  /// Require unlock only after this long in background. Do not lock for shorter absences.
  static const Duration backgroundLockThreshold = Duration(seconds: 5);

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (kDebugMode) {
      debugPrint('[LockManager] didChangeAppLifecycleState: $state');
    }

    switch (state) {
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        _backgroundTimestamp = DateTime.now();
        if (kDebugMode) debugPrint('[LockManager] backgrounded at $_backgroundTimestamp');
        break;

      case AppLifecycleState.resumed:
        if (_backgroundTimestamp == null) return;

        final elapsed = DateTime.now().difference(_backgroundTimestamp!);
        _backgroundTimestamp = null;

        if (elapsed < backgroundLockThreshold) {
          if (kDebugMode) {
            debugPrint('[LockManager] resumed: brief (${elapsed.inSeconds}s), skip lock');
          }
          return;
        }

        if (kDebugMode) {
          debugPrint('[LockManager] resumed: away ${elapsed.inSeconds}s, checking lock...');
        }
        Future.microtask(() => _requireUnlockIfNeeded());
        break;

      case AppLifecycleState.inactive:
        // Do nothing: screenshot, share sheet, notification shade, biometric prompt, etc.
        break;
      default:
        // hidden (Flutter 3.13+) or future values: do not treat as background
        break;
    }
  }

  /// Called after resume when away ≥ threshold. Checks auth + lock enabled then sets state.
  Future<void> _requireUnlockIfNeeded() async {
    if (state) return; // already showing lock, prevent double navigation

    final authState = _ref.read(authStateProvider);
    if (!authState.isAuthenticated) return;

    if (_ref.read(appLockTemporaryDisableProvider)) return;

    try {
      final service = _ref.read(appLockServiceProvider);
      final results = await Future.wait<bool>([
        service.isLockEnabled(),
        service.hasPin(),
      ]);
      if (results[0] && results[1]) {
        state = true;
        if (kDebugMode) debugPrint('[LockManager] requireUnlock: showing lock screen');
      }
    } catch (e) {
      if (kDebugMode) debugPrint('[LockManager] _requireUnlockIfNeeded error: $e');
    }
  }

  /// Call when user successfully unlocks. Clears "should show lock" state.
  void unlock() {
    if (state) {
      state = false;
      if (kDebugMode) debugPrint('[LockManager] unlock: hiding lock screen');
    }
  }
}
