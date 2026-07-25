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
import 'package:shifa_patient_app_v1/features/auth/data/auth_exceptions.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/registration_provider.dart';

class CreateAccountScreen extends ConsumerStatefulWidget {
  const CreateAccountScreen({super.key});

  @override
  ConsumerState<CreateAccountScreen> createState() => _CreateAccountScreenState();
}

class _CreateAccountScreenState extends ConsumerState<CreateAccountScreen> {
  static const int _maxSmsAttempts = 3;

  final _formKey = GlobalKey<FormState>();
  final _phoneFieldKey = GlobalKey<PhoneInputFieldState>();
  final _nameController = TextEditingController();
  final _surnameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  String _phoneValue = '';
  final _confirmPasswordController = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  List<PasswordRequirementResult> _passwordRequirements = const [];
  bool _isSubmitting = false;
  /// Failed SMS OTP sends so far (1..3). After 3, force email fallback.
  int _smsFailureCount = 0;
  String? _otpError;
  bool _forceEmailFallback = false;
  /// Identifier check already passed — retries skip it.
  bool _identifierReady = false;

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

  String? _effectivePhone() {
    final raw = _phoneFieldKey.currentState?.fullPhone ?? _phoneValue;
    final normalized = normalizePhoneForSms(raw);
    if (normalized.isEmpty) return null;
    final digits = normalized.replaceAll(RegExp(r'\D'), '');
    if (digits.length < 10) return null;
    return normalized;
  }

  bool _isUzbekPhone(String? phone) {
    if (phone == null || phone.trim().isEmpty) return false;
    final digits = phone.replaceAll(RegExp(r'\D'), '');
    if (digits.startsWith('998') && digits.length == 12) return true;
    if (digits.length == 9 && digits.startsWith('9')) return true;
    return false;
  }

  Future<String?> _promptEmailForSmsFallback(AppLocalizations l10n) async {
    final emailCtrl = TextEditingController(
      text: _emailController.text.trim(),
    );
    try {
      return showDialog<String>(
        context: context,
        barrierDismissible: false,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.translate('smsUnavailableTitle')),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(l10n.translate('smsFailedUseEmailMessage')),
              const SizedBox(height: 16),
              TextField(
                controller: emailCtrl,
                keyboardType: TextInputType.emailAddress,
                autofillHints: const [AutofillHints.email],
                decoration: InputDecoration(
                  labelText: l10n.email,
                  hintText: 'example@email.com',
                  prefixIcon: const Icon(Icons.email),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: Text(l10n.cancel),
            ),
            FilledButton(
              onPressed: () {
                final v = emailCtrl.text.trim().toLowerCase();
                if (!_isValidEmail(v)) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(content: Text(l10n.invalidEmail)),
                  );
                  return;
                }
                Navigator.pop(ctx, v);
              },
              child: Text(l10n.translate('continueWithEmail')),
            ),
          ],
        ),
      );
    } finally {
      emailCtrl.dispose();
    }
  }

  void _goToOtpVerify({
    required RegistrationOtpChannel channel,
    required String destination,
    required String? email,
    required String? phone,
  }) {
    ref.read(registrationProvider.notifier).updateStep1(
          firstName: _nameController.text,
          lastName: _surnameController.text,
          phone: phone,
          email: email,
          password: _passwordController.text,
          otpChannel: channel,
        );
    ref.read(registerOtpVerificationProvider.notifier).state =
        RegisterOtpVerificationState(
      channel: channel,
      destination: destination,
    );
    context.push(AppRoutes.registerOtpVerify);
  }

  Future<void> _sendEmailOtpAndContinue({
    required AuthRepository repo,
    required String email,
    required String? phone,
  }) async {
    await repo.sendEmailOtp(email).timeout(
      const Duration(seconds: 25),
      onTimeout: () => throw Exception(
        'Connection timed out. Check your internet and try again.',
      ),
    );
    if (!mounted) return;
    setState(() {
      _otpError = null;
      _smsFailureCount = 0;
      _forceEmailFallback = false;
    });
    _goToOtpVerify(
      channel: RegistrationOtpChannel.email,
      destination: email,
      email: email,
      phone: phone,
    );
  }

  /// SMS only — used for attempts 1–3. Does not fall back to email here.
  Future<bool> _trySendSmsOtp({
    required AuthRepository repo,
    required String phone,
  }) async {
    final channel = await repo.sendSmsOtp(phone).timeout(
      const Duration(seconds: 30),
      onTimeout: () => throw Exception(
        'Connection timed out. Check your internet and try again.',
      ),
    );
    if (!mounted) return false;
    if (channel == RegistrationOtpChannel.email) {
      // Server unexpectedly fell back — treat as SMS failure for client policy.
      throw SmsOtpUnavailableException();
    }
    setState(() {
      _otpError = null;
      _smsFailureCount = 0;
      _forceEmailFallback = false;
    });
    _goToOtpVerify(
      channel: RegistrationOtpChannel.sms,
      destination: phone,
      email: null,
      phone: phone,
    );
    return true;
  }

  Future<void> _handleSmsFailure(Object e, AppLocalizations l10n) async {
    final message = userFriendlyError(l10n, e, logContext: 'Create account SMS');
    final nextCount = _smsFailureCount + 1;
    if (!mounted) return;

    if (e is OtpRateLimitException) {
      setState(() {
        _otpError = message;
        // Rate limit is not an SMS-provider failure — keep retry available
        // without advancing toward email fallback.
      });
      return;
    }

    if (nextCount >= _maxSmsAttempts) {
      setState(() {
        _smsFailureCount = nextCount;
        _forceEmailFallback = true;
        _otpError = l10n.translate('smsFailedUseEmailMessage');
        _isSubmitting = false;
      });
      final email = await _promptEmailForSmsFallback(l10n);
      if (!mounted) return;
      if (email == null) {
        // User cancelled — leave error + enable continue-with-email via button.
        return;
      }
      _emailController.text = email;
      setState(() => _isSubmitting = true);
      try {
        final repo = ref.read(authRepositoryProvider);
        await _sendEmailOtpAndContinue(
          repo: repo,
          email: email,
          phone: _effectivePhone(),
        );
      } catch (emailErr) {
        if (mounted) {
          setState(() {
            _otpError = userFriendlyError(
              l10n,
              emailErr,
              logContext: 'Create account email fallback',
            );
          });
        }
      } finally {
        if (mounted) setState(() => _isSubmitting = false);
      }
      return;
    }

    setState(() {
      _smsFailureCount = nextCount;
      _otpError = message;
      _forceEmailFallback = false;
    });
  }

  Future<bool> _ensureIdentifierOk({
    required AuthRepository repo,
    required String? phone,
    required String emailTrimmed,
    required bool hasEmail,
    required AppLocalizations l10n,
  }) async {
    if (_identifierReady) return true;

    final result = await repo
        .checkIdentifier(
          phone: phone,
          email: hasEmail ? emailTrimmed : null,
        )
        .timeout(
          const Duration(seconds: 25),
          onTimeout: () => throw Exception(
            'Connection timed out. Check your internet and try again.',
          ),
        );
    if (!mounted) return false;

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
      return false;
    }
    if (result.isDoctor) {
      final yes = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(l10n.translate('existingDoctorTitle')),
          content: Text(
            l10n
                .translate('existingDoctorMessage')
                .replaceAll(
                  '{{name}}',
                  '${result.firstName ?? ''} ${result.lastName ?? ''}'.trim(),
                ),
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
          'phone': phone,
          'email': emailTrimmed.isEmpty ? null : emailTrimmed,
          'firstName': result.firstName ?? '',
          'lastName': result.lastName ?? '',
        });
      }
      return false;
    }

    _identifierReady = true;
    return true;
  }

  Future<void> _onPrimaryAction() async {
    final l10n = AppLocalizations.of(context)!;
    if (_forceEmailFallback) {
      await _continueWithEmailFallback(l10n);
      return;
    }
    if (_smsFailureCount > 0 && _smsFailureCount < _maxSmsAttempts) {
      await _retrySms(l10n);
      return;
    }
    await _startRegistration(l10n);
  }

  Future<void> _continueWithEmailFallback(AppLocalizations l10n) async {
    var email = _emailController.text.trim().toLowerCase();
    if (!_isValidEmail(email)) {
      final prompted = await _promptEmailForSmsFallback(l10n);
      if (prompted == null || !mounted) return;
      email = prompted;
      _emailController.text = email;
    }

    setState(() {
      _isSubmitting = true;
      _otpError = null;
    });
    try {
      final repo = ref.read(authRepositoryProvider);
      await _sendEmailOtpAndContinue(
        repo: repo,
        email: email,
        phone: _effectivePhone(),
      );
    } catch (e) {
      if (mounted) {
        setState(() {
          _otpError =
              userFriendlyError(l10n, e, logContext: 'Create account email');
        });
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _retrySms(AppLocalizations l10n) async {
    if (!_formKey.currentState!.validate()) return;
    final phone = _effectivePhone();
    if (phone == null || !_isUzbekPhone(phone)) {
      setState(() {
        _otpError = l10n.translate('emailRequiredForForeignPhone');
      });
      return;
    }

    setState(() {
      _isSubmitting = true;
      _otpError = null;
    });
    try {
      final repo = ref.read(authRepositoryProvider);
      await _trySendSmsOtp(repo: repo, phone: phone);
    } catch (e) {
      await _handleSmsFailure(e, l10n);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _startRegistration(AppLocalizations l10n) async {
    if (!_formKey.currentState!.validate()) return;

    final emailTrimmed = _emailController.text.trim();
    final phone = _effectivePhone();
    final hasEmail = emailTrimmed.isNotEmpty;

    if (!hasEmail && phone == null) {
      setState(() => _otpError = l10n.translate('phoneOrEmailRequired'));
      return;
    }
    if (!hasEmail && !_isUzbekPhone(phone)) {
      setState(() => _otpError = l10n.translate('emailRequiredForForeignPhone'));
      return;
    }

    setState(() {
      _isSubmitting = true;
      _otpError = null;
    });

    try {
      final repo = ref.read(authRepositoryProvider);
      final ok = await _ensureIdentifierOk(
        repo: repo,
        phone: phone,
        emailTrimmed: emailTrimmed,
        hasEmail: hasEmail,
        l10n: l10n,
      );
      if (!ok || !mounted) return;

      // Email-only registration (no UZ phone): send email OTP directly.
      if (phone == null || !_isUzbekPhone(phone)) {
        try {
          await _sendEmailOtpAndContinue(
            repo: repo,
            email: emailTrimmed,
            phone: phone,
          );
        } catch (e) {
          if (mounted) {
            setState(() {
              _otpError =
                  userFriendlyError(l10n, e, logContext: 'Create account');
            });
          }
        }
        return;
      }

      // UZ phone: SMS attempts 1–3 (even if email was typed).
      try {
        await _trySendSmsOtp(repo: repo, phone: phone);
      } catch (e) {
        await _handleSmsFailure(e, l10n);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _otpError =
              userFriendlyError(l10n, e, logContext: 'Create account');
        });
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  String _primaryButtonLabel(AppLocalizations l10n) {
    if (_isSubmitting) return l10n.translate('sendingVerificationCode');
    if (_forceEmailFallback) return l10n.translate('continueWithEmail');
    if (_smsFailureCount > 0 && _smsFailureCount < _maxSmsAttempts) {
      return l10n.translate('tryAgain');
    }
    return l10n.translate('continueToVerification');
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
                validator: (value) =>
                    value?.isEmpty ?? true ? l10n.translate('required') : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _surnameController,
                decoration: InputDecoration(labelText: l10n.surname),
                validator: (value) =>
                    value?.isEmpty ?? true ? l10n.translate('required') : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                keyboardType: TextInputType.emailAddress,
                decoration: InputDecoration(
                  labelText: _forceEmailFallback
                      ? l10n.email
                      : '${l10n.email} (${l10n.optional})',
                  hintText: 'example@email.com',
                  prefixIcon: const Icon(Icons.email),
                ),
                validator: (value) {
                  final v = value?.trim() ?? '';
                  if (_forceEmailFallback) {
                    if (v.isEmpty) return l10n.translate('emailRequired');
                    if (!_isValidEmail(v)) return l10n.invalidEmail;
                    return null;
                  }
                  if (v.isEmpty) return null;
                  if (!_isValidEmail(v)) return l10n.invalidEmail;
                  return null;
                },
              ),
              const SizedBox(height: 16),
              PhoneInputField(
                key: _phoneFieldKey,
                labelText: l10n.phoneNumber,
                onChanged: (fullPhone) =>
                    setState(() => _phoneValue = fullPhone),
                validator: (value) {
                  final emailEmpty = _emailController.text.trim().isEmpty;
                  if (_forceEmailFallback) return null;
                  if (emailEmpty) {
                    if (value == null || value.isEmpty) {
                      return l10n.translate('phoneOrEmailRequired');
                    }
                    final digits = value.replaceAll(RegExp(r'\D'), '');
                    if (digits.length < 10) {
                      return l10n.translate('invalidPhone');
                    }
                    if (!_isUzbekPhone(value)) {
                      return l10n.translate('emailRequiredForForeignPhone');
                    }
                  } else if (value != null && value.isNotEmpty) {
                    final digits = value.replaceAll(RegExp(r'\D'), '');
                    if (digits.length < 10) {
                      return l10n.translate('invalidPhone');
                    }
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
                    icon: Icon(_obscurePassword
                        ? Icons.visibility
                        : Icons.visibility_off),
                    onPressed: () =>
                        setState(() => _obscurePassword = !_obscurePassword),
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
                ..._passwordRequirements.map(
                  (r) => Padding(
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
                  ),
                ),
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
                    icon: Icon(_obscureConfirmPassword
                        ? Icons.visibility
                        : Icons.visibility_off),
                    onPressed: () => setState(
                      () => _obscureConfirmPassword = !_obscureConfirmPassword,
                    ),
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
              if (_otpError != null) ...[
                const SizedBox(height: 16),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFEBEE),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: const Color(0xFFEF9A9A)),
                  ),
                  child: Text(
                    _otpError!,
                    style: const TextStyle(
                      color: Color(0xFFC62828),
                      fontSize: 14,
                      height: 1.35,
                    ),
                  ),
                ),
              ],
              const SizedBox(height: 32),
              ShifaPrimaryButton(
                label: _primaryButtonLabel(l10n),
                isLoading: _isSubmitting,
                onPressed: _isSubmitting ? null : _onPrimaryAction,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
