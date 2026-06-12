import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/user_model.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/services/login_attempt_service.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/notifications/providers/notifications_provider.dart';

final loginAttemptServiceProvider = Provider<LoginAttemptService>((ref) => LoginAttemptService());

final authStateProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref);
});

class AuthNotifier extends StateNotifier<AuthState> {
  final Ref _ref;
  final Completer<void> _initialAuthCompleter = Completer<void>();

  AuthNotifier(this._ref) : super(AuthState.initial()) {
    // Integration / promo screenshot runs: skip network auth (see integration_test/promo_screens_capture_test.dart).
    if (const bool.fromEnvironment('PROMO_CAPTURE', defaultValue: false)) {
      state = AuthState(
        isAuthenticated: true,
        isLoading: false,
        isAuthReady: true,
        token: 'promo-capture',
      );
      _initialAuthCompleter.complete();
      return;
    }
    // Use a post-frame callback to avoid blocking initialization
    Future.microtask(_completeInitialAuthCheck);
  }

  /// Resolves once the first auth restore/validation pass finishes (splash waits on this).
  Future<void> ensureInitialAuthChecked() => _initialAuthCompleter.future;

  Future<void> _completeInitialAuthCheck() async {
    try {
      await _checkAuth();
    } finally {
      state = state.copyWith(isAuthReady: true);
      if (!_initialAuthCompleter.isCompleted) {
        _initialAuthCompleter.complete();
      }
    }
  }

  AuthRepository get _repository => _ref.read(authRepositoryProvider);

  Future<void> _checkAuth() async {
    try {
      final hasToken = await _repository.isAuthenticated();
      if (!hasToken) {
        state = state.copyWith(isAuthenticated: false);
        return;
      }
      // Validate token with backend to catch invalid/expired tokens (e.g. after password change in doctor app)
      final isValid = await _repository.validateToken();
      state = state.copyWith(isAuthenticated: isValid);
    } catch (e) {
      // Silently fail - assume not authenticated if check fails
      state = state.copyWith(isAuthenticated: false);
    }
  }

  Future<void> login(String username, String password) async {
    final attemptService = _ref.read(loginAttemptServiceProvider);

    if (await attemptService.isLockedOut()) {
      final sec = await attemptService.lockoutRemainingSeconds();
      final minutes = (sec / 60).ceil().clamp(1, LoginAttemptService.lockoutDurationMinutes);
      state = state.copyWith(
        isLoading: false,
        isAuthenticated: false,
        error: null,
        loginLockedUntilMinutes: minutes,
        loginAttemptsRemaining: 0,
      );
      return;
    }

    state = state.copyWith(
      isLoading: true,
      error: null,
      loginLockedUntilMinutes: null,
      loginAttemptsRemaining: null,
    );
    try {
      final result = await _repository.login(username, password);
      await attemptService.clearAttempts();
      state = state.copyWith(
        isLoading: false,
        isAuthenticated: true,
        token: result.token,
        forcePasswordReset: result.forcePasswordReset,
        error: null,
        loginLockedUntilMinutes: null,
        loginAttemptsRemaining: null,
      );
    } catch (e) {
      final justLocked = await attemptService.recordFailedAttempt();
      final remaining = await attemptService.remainingAttempts();
      final lockoutSec = await attemptService.lockoutRemainingSeconds();
      final lockoutMinutes = (lockoutSec / 60).ceil().clamp(1, LoginAttemptService.lockoutDurationMinutes);
      final errorMessage = e.toString().replaceFirst('Exception: ', '');
      state = state.copyWith(
        isLoading: false,
        error: errorMessage,
        isAuthenticated: false,
        loginAttemptsRemaining: remaining,
        loginLockedUntilMinutes: justLocked ? lockoutMinutes : null,
      );
    }
  }

  Future<void> register({
    required String firstName,
    required String lastName,
    String? email,
    String? phone,
    required String password,
    String? birthDate,
    String? gender,
    String? address,
    String? language,
    String? emailOtp,
    String? smsOtp,
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final token = await _repository.register(
        firstName: firstName,
        lastName: lastName,
        phone: phone,
        email: email,
        password: password,
        birthDate: birthDate,
        gender: gender,
        address: address,
        language: language,
        emailOtp: emailOtp,
        smsOtp: smsOtp,
      );
      state = state.copyWith(
        isLoading: false,
        isAuthenticated: true,
        token: token,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
      rethrow;
    }
  }

  /// After forgot-password flow: backend returned JWT; update auth state so user is logged in.
  void completeForgotPassword(LoginResult result) {
    state = state.copyWith(
      isLoading: false,
      isAuthenticated: true,
      token: result.token,
      error: null,
      forcePasswordReset: result.forcePasswordReset,
    );
  }

  /// Create patient account for an existing doctor after OTP verification. They use their doctor password to log in.
  Future<void> registerPatientForDoctor({
    String? phone,
    String? email,
    String? emailOtp,
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final token = await _repository.createPatientForDoctor(
        phone: phone,
        email: email,
        emailOtp: emailOtp,
      );
      state = state.copyWith(
        isLoading: false,
        isAuthenticated: true,
        token: token,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString().replaceFirst('Exception: ', ''),
      );
      rethrow;
    }
  }

  Future<void> logout() async {
    // Only logout if currently authenticated to prevent loops
    if (state.isAuthenticated) {
      try {
        await _ref.read(notificationsRepositoryProvider).updateFcmToken('');
      } catch (_) {
        // Ignore if backend does not support FCM token yet
      }
      // Clear app lock PIN and disable lock so user is not stuck after reinstall or password change
      try {
        final lockService = _ref.read(appLockServiceProvider);
        await lockService.clearPin();
        await lockService.setLockEnabled(false);
        await _ref.read(appLockStateProvider.notifier).setLockEnabled(false);
      } catch (_) {
        // Ignore app lock errors during logout
      }
      await _repository.logout();
      state = AuthState.initial();
    }
  }

  void markPasswordResetDone() {
    state = state.copyWith(forcePasswordReset: false);
  }

  /// Re-read lockout state (e.g. for countdown on login screen).
  Future<void> refreshLoginLockout() async {
    final attemptService = _ref.read(loginAttemptServiceProvider);
    if (!await attemptService.isLockedOut()) {
      state = state.copyWith(clearLoginAttemptsRemaining: true, clearLoginLockedUntilMinutes: true);
      return;
    }
    final sec = await attemptService.lockoutRemainingSeconds();
    final minutes = (sec / 60).ceil().clamp(0, LoginAttemptService.lockoutDurationMinutes);
    state = state.copyWith(loginLockedUntilMinutes: minutes > 0 ? minutes : null, clearLoginAttemptsRemaining: true);
  }
}

class AuthState {
  final bool isAuthenticated;
  final bool isLoading;
  /// False until the first stored-token check completes on cold start.
  final bool isAuthReady;
  final String? token;
  final UserModel? user;
  final String? error;
  final bool forcePasswordReset;
  /// Remaining login attempts before lockout (null when not applicable).
  final int? loginAttemptsRemaining;
  /// When locked, minutes until user can try again.
  final int? loginLockedUntilMinutes;

  AuthState({
    required this.isAuthenticated,
    required this.isLoading,
    this.isAuthReady = true,
    this.token,
    this.user,
    this.error,
    this.forcePasswordReset = false,
    this.loginAttemptsRemaining,
    this.loginLockedUntilMinutes,
  });

  factory AuthState.initial() {
    return AuthState(
      isAuthenticated: false,
      isLoading: false,
      isAuthReady: false,
      forcePasswordReset: false,
    );
  }

  AuthState copyWith({
    bool? isAuthenticated,
    bool? isLoading,
    bool? isAuthReady,
    String? token,
    UserModel? user,
    String? error,
    bool? forcePasswordReset,
    int? loginAttemptsRemaining,
    int? loginLockedUntilMinutes,
    bool clearLoginAttemptsRemaining = false,
    bool clearLoginLockedUntilMinutes = false,
  }) {
    return AuthState(
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      isLoading: isLoading ?? this.isLoading,
      isAuthReady: isAuthReady ?? this.isAuthReady,
      token: token ?? this.token,
      user: user ?? this.user,
      error: error,
      forcePasswordReset: forcePasswordReset ?? this.forcePasswordReset,
      loginAttemptsRemaining: clearLoginAttemptsRemaining ? null : (loginAttemptsRemaining ?? this.loginAttemptsRemaining),
      loginLockedUntilMinutes: clearLoginLockedUntilMinutes ? null : (loginLockedUntilMinutes ?? this.loginLockedUntilMinutes),
    );
  }

  bool get isLoginLocked => loginLockedUntilMinutes != null && loginLockedUntilMinutes! > 0;
}
