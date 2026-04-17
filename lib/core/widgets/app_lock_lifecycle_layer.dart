import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/providers/language_provider.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/theme/app_theme.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_lock_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';

/// Root-level lock gate: when app must be locked, we REPLACE the entire app
/// with the PIN screen (no overlay, no Stack). This guarantees:
/// - No overlay hit-test or z-order issues (nothing can block the PIN screen).
/// - PIN screen is the only widget tree when locked, so it is always interactive.
///
/// Lifecycle (handled by [LockManager], not here):
/// - Only [paused] / [detached] count as background; [inactive] (screenshot, etc.) is ignored.
/// - Lock required only when background duration ≥ 5 seconds.
/// - Cold start: if authenticated and lock enabled, show PIN after first frame.
///
/// Logging (kDebugMode): lock show/hide for debugging.
class AppLockLifecycleLayer extends ConsumerStatefulWidget {
  final Widget child;

  const AppLockLifecycleLayer({super.key, required this.child});

  @override
  ConsumerState<AppLockLifecycleLayer> createState() => _AppLockLifecycleLayerState();
}

class _AppLockLifecycleLayerState extends ConsumerState<AppLockLifecycleLayer> {
  bool _showLockScreen = false;
  bool _coldStartCheckDone = false;

  static const _storageTimeout = Duration(seconds: 5);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _maybeShowLockOnColdStart());
  }

  Future<bool> _shouldShowLock() async {
    final lockService = ref.read(appLockServiceProvider);
    try {
      final results = await Future.wait<bool>([
        lockService.isLockEnabled(),
        lockService.hasPin(),
      ]).timeout(_storageTimeout);
      return results[0] && results[1];
    } on Exception catch (e) {
      if (kDebugMode) debugPrint('[AppLockLifecycle] storage timeout or error: $e');
      return true;
    }
  }

  Future<void> _maybeShowLockOnColdStart() async {
    if (_coldStartCheckDone || !mounted) return;

    final authState = ref.read(authStateProvider);
    if (!authState.isAuthenticated) return;
    _coldStartCheckDone = true;
    if (ref.read(appLockTemporaryDisableProvider)) return;

    if (kDebugMode) debugPrint('[AppLockLifecycle] cold start: checking lock...');
    try {
      final show = await _shouldShowLock();
      if (mounted && show) {
        if (kDebugMode) debugPrint('[AppLockLifecycle] cold start: showing PIN screen');
        setState(() => _showLockScreen = true);
      }
    } catch (_) {
      if (kDebugMode) debugPrint('[AppLockLifecycle] cold start: error, skip lock');
    }
  }

  void _onUnlock() {
    ref.read(lockManagerProvider.notifier).unlock();
    if (mounted) {
      if (kDebugMode) debugPrint('[AppLockLifecycle] onUnlock: hiding PIN screen');
      setState(() => _showLockScreen = false);
    }
  }

  void _onForceLogin() {
    ref.read(authStateProvider.notifier).logout();
    ref.read(lockManagerProvider.notifier).unlock();
    if (mounted) {
      if (kDebugMode) debugPrint('[AppLockLifecycle] onForceLogin: logout');
      setState(() => _showLockScreen = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    // Resume-based lock from LockManager (background ≥5s). Cold start uses _showLockScreen.
    final shouldShowLockFromResume = ref.watch(lockManagerProvider);

    // Re-run cold start check when auth becomes ready (fixes race: auth loads async after first frame)
    ref.listen(authStateProvider, (prev, next) {
      if (next.isAuthenticated && !_coldStartCheckDone && mounted) {
        WidgetsBinding.instance.addPostFrameCallback((_) => _maybeShowLockOnColdStart());
      }
    });

    if (!_showLockScreen && !shouldShowLockFromResume) return widget.child;

    // Root-level gate: REPLACE entire app with PIN screen. No Stack, no overlay.
    // This is the only way to guarantee the PIN screen receives all touch events
    // and cannot be blocked by any other layer or hit-test ordering.
    final languageState = ref.watch(languageProvider);
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      locale: languageState.locale,
      supportedLocales: const [
        Locale('en'),
        Locale('de'),
        Locale('uz'),
        Locale('ru'),
      ],
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: PopScope(
        canPop: false,
        child: AppLockScreen(
          onUnlock: _onUnlock,
          onForceLogin: _onForceLogin,
        ),
      ),
    );
  }
}
