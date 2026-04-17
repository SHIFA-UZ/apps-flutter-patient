import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/utils/password_validation.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';

class ResetPasswordScreen extends ConsumerStatefulWidget {
  const ResetPasswordScreen({super.key});

  @override
  ConsumerState<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends ConsumerState<ResetPasswordScreen> {
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
    _passwordRequirements =
        PasswordValidation.getRequirementResults(_newPasswordController.text);
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

    setState(() => _isLoading = true);
    try {
      final password = _newPasswordController.text.trim();
      final repository = ref.read(authRepositoryProvider);
      
      // We need a way to reset password in repository
      // I'll add this method to AuthRepository next
      await repository.resetPassword(password);
      
      // Update auth state to mark reset as done
      ref.read(authStateProvider.notifier).markPasswordResetDone();
      
      if (mounted) {
        context.go(AppRoutes.home);
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${l10n.error}: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.translate('resetPassword') ?? 'Reset Password'),
        automaticallyImplyLeading: false, // Force stay here
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                l10n.translate('mustChangePassword') ?? 'You must change your password on first login.',
                style: const TextStyle(fontSize: 16, color: Colors.grey),
              ),
              const SizedBox(height: 24),
              TextFormField(
                controller: _newPasswordController,
                obscureText: _obscureNew,
                decoration: InputDecoration(
                  labelText: l10n.translate('newPassword') ?? 'New Password',
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
                  labelText: l10n.translate('confirmPassword') ?? 'Confirm Password',
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
                    return l10n.translate('passwordsDoNotMatch') ?? 'Passwords do not match';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 32),
              ShifaPrimaryButton(
                label: l10n.translate('savePassword') ?? 'Save & Continue',
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
