import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_service.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_exceptions.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/registration_provider.dart';

const _resendCooldownSeconds = 5 * 60;
/// Must match backend [SmsOtpService.MAX_SMS_SENDS_PER_HOUR] (initial send + resends).
const _maxSmsSends = 3;

class RegisterOtpVerifyScreen extends ConsumerStatefulWidget {
  const RegisterOtpVerifyScreen({super.key});

  @override
  ConsumerState<RegisterOtpVerifyScreen> createState() => _RegisterOtpVerifyScreenState();
}

class _RegisterOtpVerifyScreenState extends ConsumerState<RegisterOtpVerifyScreen> {
  final _codeCtrl = TextEditingController();
  bool _isLoading = false;
  int _resendSecondsRemaining = _resendCooldownSeconds;
  Timer? _resendTimer;
  /// Counts the initial SMS send as 1 once the screen opens on the SMS channel.
  int _smsSendsUsed = 1;

  @override
  void initState() {
    super.initState();
    _startResendCountdown();
    WidgetsBinding.instance.addPostFrameCallback((_) => _ensureRegistrationState());
  }

  void _ensureRegistrationState() {
    if (!mounted) return;
    final otpState = ref.read(registerOtpVerificationProvider);
    final reg = ref.read(registrationProvider);
    if (otpState == null || !reg.canRegister) {
      context.go(AppRoutes.createAccount);
      return;
    }
    // Initial SMS already sent on the previous screen.
    if (otpState.channel != RegistrationOtpChannel.sms) {
      _smsSendsUsed = 0;
    }
  }

  void _startResendCountdown() {
    _resendTimer?.cancel();
    _resendSecondsRemaining = _resendCooldownSeconds;
    _resendTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() {
        if (_resendSecondsRemaining > 0) _resendSecondsRemaining--;
      });
      if (_resendSecondsRemaining <= 0) _resendTimer?.cancel();
    });
  }

  @override
  void dispose() {
    _resendTimer?.cancel();
    _codeCtrl.dispose();
    super.dispose();
  }

  String _maskPhone(String phone) {
    final digits = phone.replaceAll(RegExp(r'\D'), '');
    if (digits.length < 6) return phone;
    final last4 = digits.substring(digits.length - 4);
    return '+998 ** *** $last4';
  }

  Future<void> _onVerify() async {
    final otpState = ref.read(registerOtpVerificationProvider);
    final reg = ref.read(registrationProvider);
    if (otpState == null || !reg.canRegister) {
      if (mounted) context.go(AppRoutes.createAccount);
      return;
    }
    final l10n = AppLocalizations.of(context)!;
    final code = _codeCtrl.text.trim().replaceAll(RegExp(r'\D'), '');
    if (code.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('invalidVerificationCode'))),
      );
      return;
    }
    setState(() => _isLoading = true);
    try {
      final isEmail = otpState.channel == RegistrationOtpChannel.email;
      await ref.read(authStateProvider.notifier).register(
            firstName: reg.firstName!,
            lastName: reg.lastName!,
            email: reg.email,
            phone: reg.phone,
            password: reg.password!,
            emailOtp: isEmail ? code : null,
            smsOtp: isEmail ? null : code,
          );
      ref.read(registerOtpVerificationProvider.notifier).state = null;
      ref.read(registrationProvider.notifier).clear();
      if (!mounted) return;
      await _showSetPinThenGoHome();
      if (mounted) context.go(AppRoutes.home);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(userFriendlyError(l10n, e, logContext: 'Verify/Register'))),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  bool _isValidEmail(String email) {
    return RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(email);
  }

  Future<String?> _promptEmailForSmsFallback(AppLocalizations l10n) async {
    final emailCtrl = TextEditingController();
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

  Future<void> _switchToEmailChannel(String email) async {
    final authRepo = ref.read(authRepositoryProvider);
    await authRepo.sendEmailOtp(email);
    ref.read(registrationProvider.notifier).setOtpChannel(RegistrationOtpChannel.email);
    // Keep phone/password; attach email for register + verify.
    final reg = ref.read(registrationProvider);
    ref.read(registrationProvider.notifier).updateStep1(
          firstName: reg.firstName ?? '',
          lastName: reg.lastName ?? '',
          phone: reg.phone,
          email: email,
          password: reg.password ?? '',
          otpChannel: RegistrationOtpChannel.email,
        );
    ref.read(registerOtpVerificationProvider.notifier).state =
        RegisterOtpVerificationState(
      channel: RegistrationOtpChannel.email,
      destination: email,
    );
  }

  Future<void> _onResendCode() async {
    final reg = ref.read(registrationProvider);
    final otpState = ref.read(registerOtpVerificationProvider);
    if (!reg.canRegister || _resendSecondsRemaining > 0 || otpState == null) return;
    final l10n = AppLocalizations.of(context)!;
    setState(() => _isLoading = true);
    try {
      final authRepo = ref.read(authRepositoryProvider);
      if (otpState.channel == RegistrationOtpChannel.email) {
        final email = reg.email?.trim();
        if (email == null || email.isEmpty) return;
        await authRepo.sendEmailOtp(email);
      } else {
        // After 3 SMS (initial + resends), stop burning credits and switch to email.
        if (_smsSendsUsed >= _maxSmsSends) {
          final existing = reg.email?.trim();
          final email = (existing != null && existing.isNotEmpty)
              ? existing
              : await _promptEmailForSmsFallback(l10n);
          if (email == null) return;
          await _switchToEmailChannel(email);
        } else {
          final phone = reg.phone?.trim();
          if (phone == null || phone.isEmpty) return;
          try {
            final channel = await authRepo.sendSmsOtp(
              phone,
              fallbackEmail: reg.email?.trim(),
            );
            if (channel == RegistrationOtpChannel.email) {
              final email = reg.email?.trim().isNotEmpty == true
                  ? reg.email!.trim()
                  : await _promptEmailForSmsFallback(l10n);
              if (email == null) return;
              await _switchToEmailChannel(email);
            } else {
              _smsSendsUsed++;
            }
          } on SmsOtpUnavailableException {
            final email = reg.email?.trim().isNotEmpty == true
                ? reg.email!.trim()
                : await _promptEmailForSmsFallback(l10n);
            if (email == null) return;
            await _switchToEmailChannel(email);
          } on OtpRateLimitException {
            final email = reg.email?.trim().isNotEmpty == true
                ? reg.email!.trim()
                : await _promptEmailForSmsFallback(l10n);
            if (email == null) return;
            await _switchToEmailChannel(email);
          }
        }
      }
      if (!mounted) return;
      _startResendCountdown();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('codeSentAgain'))),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(userFriendlyError(l10n, e, logContext: 'Resend OTP'))),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  String _formatResendTime(int seconds) {
    final m = seconds ~/ 60;
    final s = seconds % 60;
    return '${m}:${s.toString().padLeft(2, '0')}';
  }

  Future<void> _showSetPinThenGoHome() async {
    final l10n = AppLocalizations.of(context)!;
    final shouldSetPin = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.setUpPin ?? 'Set Up PIN'),
        content: Text(l10n.setUpPinDescription ?? 'Create a PIN code to secure your app.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: Text(l10n.translate('skip')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: Text(l10n.setUpPin ?? 'Set Up PIN'),
          ),
        ],
      ),
    );
    if (!context.mounted || shouldSetPin != true) return;
    final pin = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => _PinEntryDialog(
        title: l10n.setUpPin ?? 'Set Up PIN',
        hint: l10n.enterPinCode ?? 'Enter your PIN code',
      ),
    );
    if (pin == null || pin.length < 4 || !context.mounted) return;
    final confirmPin = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => _PinEntryDialog(
        title: l10n.confirmPin ?? 'Confirm PIN',
        hint: l10n.reEnterPin ?? 'Re-enter your PIN',
      ),
    );
    if (confirmPin != pin || !context.mounted) return;
    final service = ref.read(appLockServiceProvider);
    await service.setPin(pin);
    await ref.read(appLockStateProvider.notifier).setLockEnabled(true);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.pinSetSuccessfully ?? 'PIN set successfully')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final otpState = ref.watch(registerOtpVerificationProvider);
    final reg = ref.watch(registrationProvider);
    if (otpState == null || !reg.canRegister) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    final isEmail = otpState.channel == RegistrationOtpChannel.email;
    final displayTarget = isEmail
        ? otpState.destination
        : _maskPhone(otpState.destination);
    final otpMessage = isEmail
        ? l10n.translate('otpSentToEmail').replaceAll('{email}', displayTarget)
        : l10n.translate('otpSentToPhone').replaceAll('{phone}', displayTarget);
    final codeLabel = isEmail
        ? l10n.translate('enterEmailCode')
        : l10n.translate('enterSmsCode');

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            ref.read(registerOtpVerificationProvider.notifier).state = null;
            context.pop();
          },
        ),
        title: Text(l10n.translate('verifyAndCreate')),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              otpMessage,
              style: const TextStyle(fontSize: 16, height: 1.4),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _codeCtrl,
              keyboardType: TextInputType.number,
              maxLength: 6,
              decoration: InputDecoration(
                labelText: codeLabel,
                counterText: '',
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 24),
            TextButton(
              onPressed: (_isLoading || _resendSecondsRemaining > 0) ? null : _onResendCode,
              child: Text(
                _resendSecondsRemaining > 0
                    ? l10n.translate('resendCodeIn').replaceAll('{{time}}', _formatResendTime(_resendSecondsRemaining))
                    : l10n.translate('resendCode'),
              ),
            ),
            const SizedBox(height: 16),
            ShifaPrimaryButton(
              label: l10n.translate('verifyAndCreate'),
              onPressed: _isLoading ? null : _onVerify,
              isLoading: _isLoading,
            ),
          ],
        ),
      ),
    );
  }
}

class _PinEntryDialog extends StatefulWidget {
  final String title;
  final String hint;

  const _PinEntryDialog({required this.title, required this.hint});

  @override
  State<_PinEntryDialog> createState() => _PinEntryDialogState();
}

class _PinEntryDialogState extends State<_PinEntryDialog> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.title),
      content: TextField(
        controller: _controller,
        keyboardType: TextInputType.number,
        obscureText: true,
        maxLength: AppLockService.maxPinLength,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        decoration: InputDecoration(
          hintText: widget.hint,
          counterText: '',
          helperText: AppLocalizations.of(context)?.translate('pinLengthRequirement'),
        ),
        onSubmitted: (v) {
          if (AppLockService.isValidPin(v)) Navigator.pop(context, v);
        },
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(MaterialLocalizations.of(context).cancelButtonLabel),
        ),
        FilledButton(
          onPressed: () {
            final v = _controller.text;
            if (AppLockService.isValidPin(v)) Navigator.pop(context, v);
          },
          child: Text(MaterialLocalizations.of(context).okButtonLabel),
        ),
      ],
    );
  }
}
