import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/constants/assets.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/language_mini_toggle.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;
  Timer? _lockoutRefreshTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(authStateProvider.notifier).refreshLoginLockout();
    });
  }

  @override
  void dispose() {
    _lockoutRefreshTimer?.cancel();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  String get _usernameValue => _emailController.text.trim();

  Future<void> _handleLogin() async {
    if (!_formKey.currentState!.validate()) return;

    try {
      await ref.read(authStateProvider.notifier).login(
            _usernameValue,
            _passwordController.text,
          );
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(translateError(l10n, e.toString())),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 4),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final authState = ref.watch(authStateProvider);
    
    // Listen to auth state changes for error display only
    // Navigation is handled by router redirect to avoid showing empty login page
    ref.listen<AuthState>(authStateProvider, (previous, next) {
      if (!mounted) return;

      // Start or stop lockout countdown refresh when locked state changes
      if (next.isLoginLocked) {
        _lockoutRefreshTimer?.cancel();
        _lockoutRefreshTimer = Timer.periodic(const Duration(seconds: 30), (_) {
          ref.read(authStateProvider.notifier).refreshLoginLockout();
        });
      } else {
        _lockoutRefreshTimer?.cancel();
      }

      // Show error if it occurs (and optionally attempts-remaining warning in snackbar)
      if (next.error != null && next.error != (previous?.error)) {
        final msg = translateError(l10n, next.error!);
        final remaining = next.loginAttemptsRemaining;
        final warning = remaining != null && remaining > 0
            ? ' ${l10n.translate('loginAttemptsRemaining').replaceAll('{{count}}', '$remaining')}'
            : '';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(msg + warning),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    });

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: const [
          Padding(
            padding: EdgeInsets.only(right: 12),
            child: Center(child: LanguageMiniToggle()),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 60),
                // Shifa Logo
                Center(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(16),
                    child: Image.asset(
                      Assets.shifaLogoPng,
                      width: 80,
                      height: 80,
                      fit: BoxFit.contain,
                    ),
                  ),
                ),
                const SizedBox(height: 40),
                // Explicit warning: lockout or attempts remaining
                if (authState.isLoginLocked && authState.loginLockedUntilMinutes != null) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    decoration: BoxDecoration(
                      color: Colors.red.shade100,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.red.shade700),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.lock_clock, color: Colors.red.shade800, size: 24),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            l10n.translate('accountLockedTryAgainIn')
                                .replaceAll('{{minutes}}', '${authState.loginLockedUntilMinutes}'),
                            style: TextStyle(color: Colors.red.shade900, fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ] else if (authState.loginAttemptsRemaining != null &&
                    authState.loginAttemptsRemaining! > 0 &&
                    authState.loginAttemptsRemaining! < 5) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    decoration: BoxDecoration(
                      color: Colors.orange.shade100,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.orange.shade700),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.warning_amber_rounded, color: Colors.orange.shade800, size: 22),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            l10n.translate('loginAttemptsRemaining')
                                .replaceAll('{{count}}', '${authState.loginAttemptsRemaining}'),
                            style: TextStyle(color: Colors.orange.shade900, fontWeight: FontWeight.w500),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  decoration: InputDecoration(
                    labelText: l10n.email,
                    hintText: l10n.email,
                    prefixIcon: const Icon(Icons.email),
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return '${l10n.required}: ${l10n.email}';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                // Password field
                TextFormField(
                  controller: _passwordController,
                  obscureText: _obscurePassword,
                  decoration: InputDecoration(
                    labelText: l10n.password,
                    hintText: l10n.password,
                    prefixIcon: const Icon(Icons.lock),
                    suffixIcon: IconButton(
                      icon: Icon(_obscurePassword ? Icons.visibility : Icons.visibility_off),
                      onPressed: () {
                        setState(() {
                          _obscurePassword = !_obscurePassword;
                        });
                      },
                    ),
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return '${l10n.required}: ${l10n.password}';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 8),
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton(
                    onPressed: () => context.push(AppRoutes.forgotPassword),
                    child: Text(
                      l10n.forgotPassword,
                      style: const TextStyle(decoration: TextDecoration.underline),
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                // Sign In button (disabled when locked)
                ShifaPrimaryButton(
                  label: l10n.signIn,
                  onPressed: (authState.isLoading || authState.isLoginLocked) ? null : _handleLogin,
                  isLoading: authState.isLoading,
                ),
                const SizedBox(height: 16),
                // Create Account link
                TextButton(
                  onPressed: () {
                    context.push(AppRoutes.createAccount);
                  },
                  child: Text(
                    l10n.createAccount,
                    style: const TextStyle(decoration: TextDecoration.underline),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
