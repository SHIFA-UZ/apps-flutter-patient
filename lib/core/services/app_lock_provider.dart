import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_service.dart';
import 'package:shifa_patient_app_v1/core/services/lock_manager.dart';

final appLockServiceProvider = Provider<AppLockService>((ref) {
  return AppLockService();
});

/// Centralized lifecycle-based lock: only background ≥5s triggers unlock.
/// Screenshot / inactive never trigger. Observer registered here, removed on dispose.
final lockManagerProvider = StateNotifierProvider<LockManager, bool>((ref) {
  final manager = LockManager(ref);
  WidgetsBinding.instance.addObserver(manager);
  ref.onDispose(() => WidgetsBinding.instance.removeObserver(manager));
  return manager;
});

final appLockStateProvider = StateNotifierProvider<AppLockStateNotifier, bool>((ref) {
  final service = ref.watch(appLockServiceProvider);
  return AppLockStateNotifier(service);
});

/// Provider to track if app lock should be temporarily disabled
/// (e.g., during file operations like camera, file picker, uploads)
final appLockTemporaryDisableProvider = StateNotifierProvider<AppLockTemporaryDisableNotifier, bool>((ref) {
  return AppLockTemporaryDisableNotifier();
});

/// Tracks "needs unlock" from lifecycle (paused/inactive). Set on backgrounding, cleared on unlock.
/// Used by the root [AppLockLifecycleLayer] to show PIN overlay on resume.
final appLockNeedsUnlockProvider = StateNotifierProvider<AppLockNeedsUnlockNotifier, bool>((ref) {
  return AppLockNeedsUnlockNotifier();
});

/// When the app went to background (paused/inactive). Used to ignore brief interruptions like screenshots.
final appLockPausedAtProvider = StateProvider<DateTime?>((ref) => null);

class AppLockNeedsUnlockNotifier extends StateNotifier<bool> {
  AppLockNeedsUnlockNotifier() : super(false);

  void setNeedsUnlock() {
    state = true;
  }

  void clearNeedsUnlock() {
    state = false;
  }
}

class AppLockStateNotifier extends StateNotifier<bool> {
  final AppLockService _service;

  AppLockStateNotifier(this._service) : super(false) {
    _checkLockStatus();
  }

  Future<void> _checkLockStatus() async {
    final isLocked = await _service.isLockEnabled();
    state = isLocked;
  }

  Future<void> setLockEnabled(bool enabled) async {
    await _service.setLockEnabled(enabled);
    state = enabled;
  }

  Future<bool> isLockEnabled() async {
    return await _service.isLockEnabled();
  }
}

class AppLockTemporaryDisableNotifier extends StateNotifier<bool> {
  AppLockTemporaryDisableNotifier() : super(false);

  /// Temporarily disable app lock (e.g., during file operations)
  void disable() {
    state = true;
  }

  /// Re-enable app lock
  void enable() {
    state = false;
  }
}
