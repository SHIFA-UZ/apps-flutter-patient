import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/utils/password_validation.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';

class ForgotPasswordNewPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordNewPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordNewPasswordScreen> createState() => _ForgotPasswordNewPasswordScreenState();
}

class _ForgotPasswordNewPasswordScreenState extends ConsumerState<ForgotPasswordNewPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _newPasswordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _obscureNew = true;
  bool _obscureConfirm = true;
  bool _isLoading = false;
  List<PasswordRequirementResult> _passwordRequirements = const [];

  @override
  void initState() {
    super.initState();
    _newPasswordController.addListener(_onPasswordChanged);
  }

  void _onPasswordChanged() {
    setState(() {
      _passwordRequirements =
          PasswordValidation.getRequirementResults(_newPasswordController.text);
    });
  }

  @override
  void dispose() {
    _newPasswordController.removeListener(_onPasswordChanged);
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    final state = ref.read(forgotPasswordFlowProvider);
    if (state == null) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('errorSessionExpiredPleaseStartAgain'))),
        );
        ref.read(forgotPasswordFlowProvider.notifier).state = null;
        context.go(AppRoutes.forgotPassword);
      }
      return;
    }

    final l10n = AppLocalizations.of(context)!;
    final newPassword = _newPasswordController.text.trim();
    setState(() => _isLoading = true);
    try {
      final repository = ref.read(authRepositoryProvider);
      LoginResult result;
      if (state.email != null && state.emailOtpCode != null) {
        result = await repository.forgotPasswordResetWithEmail(
          email: state.email!,
          emailOtp: state.emailOtpCode!,
          newPassword: newPassword,
        );
      } else if (state.idToken != null) {
        result = await repository.forgotPasswordReset(
          idToken: state.idToken!,
          newPassword: newPassword,
        );
      } else {
        throw Exception(l10n.translate('errorSessionExpiredPleaseStartAgain'));
      }
      ref.read(forgotPasswordFlowProvider.notifier).state = null;
      ref.read(authStateProvider.notifier).completeForgotPassword(result);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.translate('passwordResetSuccess')),
            backgroundColor: Colors.green,
          ),
        );
        context.go(AppRoutes.home);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(translateError(l10n, e.toString())),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final state = ref.watch(forgotPasswordFlowProvider);
    if (state == null || (state.idToken == null && state.emailOtpCode == null)) {
      return Scaffold(
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const CircularProgressIndicator(),
              const SizedBox(height: 16),
              Text(l10n.translate('error')),
            ],
          ),
        ),
      );
    }
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: Text(l10n.translate('resetPassword')),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                l10n.translate('enterNewPassword'),
                style: const TextStyle(fontSize: 16, height: 1.4),
              ),
              const SizedBox(height: 24),
              TextFormField(
                controller: _newPasswordController,
                obscureText: _obscureNew,
                decoration: InputDecoration(
                  labelText: l10n.translate('newPassword'),
                  prefixIcon: const Icon(Icons.lock),
                  suffixIcon: IconButton(
                    icon: Icon(_obscureNew ? Icons.visibility : Icons.visibility_off),
                    onPressed: () => setState(() => _obscureNew = !_obscureNew),
                  ),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return l10n.translate('passwordRequired');
                  }
                  final errorKey = PasswordValidation.validate(value);
                  if (errorKey != null) {
                    return l10n.translate(errorKey);
                  }
                  return null;
                },
              ),
              const SizedBox(height: 8),
              ..._passwordRequirements.map((r) => Padding(
                    padding: const EdgeInsets.only(left: 4, bottom: 2),
                    child: Row(
                      children: [
                        Icon(
                          r.satisfied ? Icons.check_circle : Icons.cancel,
                          size: 16,
                          color: r.satisfied
                              ? const Color(0xFF17C3B2)
                              : Colors.grey,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          l10n.translate(r.l10nKey),
                          style: TextStyle(
                            fontSize: 12,
                            color: r.satisfied
                                ? const Color(0xFF17C3B2)
                                : Colors.grey,
                          ),
                        ),
                      ],
                    ),
                  )),
              const SizedBox(height: 16),
              TextFormField(
                controller: _confirmPasswordController,
                obscureText: _obscureConfirm,
                decoration: InputDecoration(
                  labelText: l10n.translate('confirmPassword'),
                  prefixIcon: const Icon(Icons.lock),
                  suffixIcon: IconButton(
                    icon: Icon(_obscureConfirm ? Icons.visibility : Icons.visibility_off),
                    onPressed: () => setState(() => _obscureConfirm = !_obscureConfirm),
                  ),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) {
                    return l10n.translate('pleaseConfirmPassword');
                  }
                  if (v != _newPasswordController.text) {
                    return l10n.translate('passwordsDoNotMatch');
                  }
                  return null;
                },
              ),
              const SizedBox(height: 32),
              ShifaPrimaryButton(
                label: l10n.translate('resetPassword'),
                onPressed: _isLoading ? null : _handleSubmit,
                isLoading: _isLoading,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
