import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/utils/password_validation.dart';
import 'package:shifa_patient_app_v1/core/widgets/phone_input_field.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/registration_provider.dart';

class CreateAccountScreen extends ConsumerStatefulWidget {
  const CreateAccountScreen({super.key});

  @override
  ConsumerState<CreateAccountScreen> createState() => _CreateAccountScreenState();
}

class _CreateAccountScreenState extends ConsumerState<CreateAccountScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _surnameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  String _phoneValue = '';
  final _confirmPasswordController = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  int _currentStep = 0;
  List<PasswordRequirementResult> _passwordRequirements = const [];
  bool _isCheckingDoctor = false;

  @override
  void initState() {
    super.initState();
    _passwordController.addListener(_onPasswordChanged);
  }

  void _onPasswordChanged() {
    setState(() {
      _passwordRequirements =
          PasswordValidation.getRequirementResults(_passwordController.text);
    });
  }

  @override
  void dispose() {
    _passwordController.removeListener(_onPasswordChanged);
    _nameController.dispose();
    _surnameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  bool _isValidEmail(String email) {
    return RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(email);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.createAccount),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _nameController,
                decoration: InputDecoration(labelText: l10n.name),
                validator: (value) => value?.isEmpty ?? true ? l10n.translate('required') : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _surnameController,
                decoration: InputDecoration(labelText: l10n.surname),
                validator: (value) => value?.isEmpty ?? true ? l10n.translate('required') : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                keyboardType: TextInputType.emailAddress,
                decoration: InputDecoration(
                  labelText: l10n.translate('emailOptional'),
                  hintText: 'example@email.com',
                  prefixIcon: const Icon(Icons.email),
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return l10n.translate('emailRequired');
                  }
                  if (!_isValidEmail(value)) {
                    return l10n.invalidEmail;
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              PhoneInputField(
                labelText: l10n.phoneNumber,
                onChanged: (fullPhone) => setState(() => _phoneValue = fullPhone),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return l10n.translate('phoneNumberRequired');
                  }
                  final digits = value.replaceAll(RegExp(r'\D'), '');
                  if (digits.length < 10) {
                    return l10n.translate('invalidPhone');
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _passwordController,
                obscureText: _obscurePassword,
                decoration: InputDecoration(
                  labelText: l10n.translate('password'),
                  prefixIcon: const Icon(Icons.lock),
                  suffixIcon: IconButton(
                    icon: Icon(_obscurePassword ? Icons.visibility : Icons.visibility_off),
                    onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
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
              if (_passwordController.text.isNotEmpty) ...[
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
                const SizedBox(height: 8),
              ],
              const SizedBox(height: 16),
              TextFormField(
                controller: _confirmPasswordController,
                obscureText: _obscureConfirmPassword,
                decoration: InputDecoration(
                  labelText: l10n.translate('confirmPassword'),
                  prefixIcon: const Icon(Icons.lock_outline),
                  suffixIcon: IconButton(
                    icon: Icon(_obscureConfirmPassword ? Icons.visibility : Icons.visibility_off),
                    onPressed: () => setState(() => _obscureConfirmPassword = !_obscureConfirmPassword),
                  ),
                ),
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return l10n.translate('pleaseConfirmPassword');
                  }
                  if (value != _passwordController.text) {
                    return l10n.passwordsDoNotMatch;
                  }
                  return null;
                },
              ),
              const SizedBox(height: 32),
              // Step indicators
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(2, (index) => Container(
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: index == _currentStep ? const Color(0xFF17C3B2) : Colors.grey[300],
                  ),
                )),
              ),
              const SizedBox(height: 32),
              ShifaPrimaryButton(
                label: l10n.next,
                isLoading: _isCheckingDoctor,
                onPressed: _isCheckingDoctor
                    ? null
                    : () async {
                        if (!_formKey.currentState!.validate()) return;
                        setState(() => _isCheckingDoctor = true);
                        try {
                          final repo = ref.read(authRepositoryProvider);
                          final result = await repo.checkIdentifier(
                            phone: _phoneValue,
                            email: _emailController.text.trim().isEmpty
                                ? null
                                : _emailController.text.trim(),
                          );
                          if (!mounted) return;
                          if (result.isPatient) {
                            await showDialog<bool>(
                              context: context,
                              builder: (ctx) => AlertDialog(
                                title: Text(l10n.translate('accountAlreadyExists')),
                                content: Text(l10n.translate('existingPatientMessage')),
                                actions: [
                                  TextButton(
                                    onPressed: () => Navigator.pop(ctx, false),
                                    child: Text(l10n.translate('ok')),
                                  ),
                                ],
                              ),
                            );
                            return;
                          }
                          if (result.isDoctor) {
                            final yes = await showDialog<bool>(
                              context: context,
                              builder: (ctx) => AlertDialog(
                                title: Text(l10n.translate('existingDoctorTitle')),
                                content: Text(
                                  l10n.translate('existingDoctorMessage')
                                      .replaceAll('{{name}}', '${result.firstName ?? ''} ${result.lastName ?? ''}'.trim()),
                                ),
                                actions: [
                                  TextButton(
                                    onPressed: () => Navigator.pop(ctx, false),
                                    child: Text(l10n.cancel),
                                  ),
                                  FilledButton(
                                    onPressed: () => Navigator.pop(ctx, true),
                                    child: Text(l10n.translate('createPatientAccount')),
                                  ),
                                ],
                              ),
                            );
                            if (yes == true && mounted) {
                              context.push(AppRoutes.confirmDoctorToPatient, extra: {
                                'phone': _phoneValue,
                                'email': _emailController.text.trim().isEmpty
                                    ? null
                                    : _emailController.text.trim(),
                                'firstName': result.firstName ?? '',
                                'lastName': result.lastName ?? '',
                              });
                            }
                            return;
                          }
                          ref.read(registrationProvider.notifier).updateStep1(
                            firstName: _nameController.text,
                            lastName: _surnameController.text,
                            phone: _phoneValue,
                            email: _emailController.text.trim().isEmpty
                                ? null
                                : _emailController.text.trim(),
                            password: _passwordController.text,
                          );
                          if (mounted) context.push(AppRoutes.accountInfo);
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
                          if (mounted) setState(() => _isCheckingDoctor = false);
                        }
                      },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
