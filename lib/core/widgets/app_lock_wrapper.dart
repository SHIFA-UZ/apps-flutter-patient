import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_lock_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';

class AppLockWrapper extends ConsumerStatefulWidget {
  final Widget child;

  const AppLockWrapper({super.key, required this.child});

  @override
  ConsumerState<AppLockWrapper> createState() => _AppLockWrapperState();
}

class _AppLockWrapperState extends ConsumerState<AppLockWrapper> {
  bool _isLocked = false;
  bool _isInitialized = false;
  bool _prevLockEnabled = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _initializeLock());
  }

  Future<void> _initializeLock() async {
    if (!mounted) return;
    try {
      final authState = ref.read(authStateProvider);
      if (!authState.isAuthenticated) {
        if (mounted) setState(() { _isInitialized = true; _isLocked = false; });
        return;
      }
      final lockService = ref.read(appLockServiceProvider);
      final isLockEnabled = await lockService.isLockEnabled();
      if (mounted) {
        setState(() {
          _isInitialized = true;
          _isLocked = false;
          _prevLockEnabled = isLockEnabled;
        });
      }
    } catch (_) {
      if (mounted) setState(() { _isInitialized = true; _isLocked = false; });
    }
  }

  void _onUnlock() {
    if (mounted) setState(() => _isLocked = false);
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authStateProvider);
    final lockEnabled = ref.watch(appLockStateProvider);
    
    // Don't show lock if not authenticated
    if (!authState.isAuthenticated) {
      return widget.child;
    }

    // Wait for initialization - show white screen with loading
    if (!_isInitialized) {
      return const Scaffold(
        backgroundColor: Colors.white,
        body: Center(
          child: CircularProgressIndicator(
            valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF17C3B2)),
          ),
        ),
      );
    }

    // When user enables lock from settings (lockEnabled transitions false→true), lock immediately.
    // Do NOT run when user just unlocked with PIN (would re-lock immediately).
    final lockJustEnabled = lockEnabled && !_prevLockEnabled;
    if (lockJustEnabled && !_isLocked && authState.isAuthenticated) {
      WidgetsBinding.instance.addPostFrameCallback((_) async {
        if (!mounted) return;
        try {
          final lockService = ref.read(appLockServiceProvider);
          final hasPin = await lockService.hasPin();
          if (hasPin && mounted) {
            setState(() {
              _isLocked = true;
            });
          }
        } catch (e) {
          // If check fails, don't lock
        }
      });
    }
    _prevLockEnabled = lockEnabled;

    // If lock is disabled, make sure we're unlocked
    if (!lockEnabled && _isLocked) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() {
            _isLocked = false;
          });
        }
      });
    }

    // Show lock screen if locked
    if (_isLocked) {
      return AppLockScreen(
        onUnlock: _onUnlock,
        onForceLogin: () {
          ref.read(authStateProvider.notifier).logout();
          if (mounted) setState(() => _isLocked = false);
        },
      );
    }

    // Show main app
    return widget.child;
  }
}
