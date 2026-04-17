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
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';

const _resendCooldownSeconds = 5 * 60; // 5 minutes

class DoctorOtpVerifyScreen extends ConsumerStatefulWidget {
  const DoctorOtpVerifyScreen({super.key});

  @override
  ConsumerState<DoctorOtpVerifyScreen> createState() => _DoctorOtpVerifyScreenState();
}

class _DoctorOtpVerifyScreenState extends ConsumerState<DoctorOtpVerifyScreen> {
  final _phoneCodeCtrl = TextEditingController();
  final _emailCodeCtrl = TextEditingController();
  bool _isLoading = false;
  int _resendSecondsRemaining = _resendCooldownSeconds;
  Timer? _resendTimer;

  @override
  void initState() {
    super.initState();
    _startResendCountdown();
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
    _phoneCodeCtrl.dispose();
    _emailCodeCtrl.dispose();
    super.dispose();
  }

  Future<void> _onVerify() async {
    final state = ref.read(doctorOtpVerificationProvider);
    if (state == null) {
      if (mounted) context.pop();
      return;
    }
    final l10n = AppLocalizations.of(context)!;
    final emailCode = _emailCodeCtrl.text.trim().replaceAll(RegExp(r'\D'), '');
    if (emailCode.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('invalidVerificationCode'))),
      );
      return;
    }
    setState(() => _isLoading = true);
    try {
      await ref.read(authStateProvider.notifier).registerPatientForDoctor(
            phone: state.phone,
            email: state.email,
            emailOtp: emailCode,
          );
      ref.read(doctorOtpVerificationProvider.notifier).state = null;
      if (!mounted) return;
      await _showSetPinThenGoHome();
      if (mounted) context.go(AppRoutes.home);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(userFriendlyError(l10n, e, logContext: 'Verify OTP'))),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _onResendCode() async {
    final state = ref.read(doctorOtpVerificationProvider);
    if (state == null || _resendSecondsRemaining > 0) return;
    final email = state.email?.trim();
    if (email == null || email.isEmpty) return;
    final l10n = AppLocalizations.of(context)!;
    setState(() => _isLoading = true);
    try {
      final authRepo = ref.read(authRepositoryProvider);
      await authRepo.sendEmailOtp(email);
      if (!mounted) return;
      _startResendCountdown();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('codeSentAgain'))),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(userFriendlyError(l10n, e, logContext: 'Resend code'))),
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
    if (pin == null || !AppLockService.isValidPin(pin) || !context.mounted) return;
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
    final state = ref.watch(doctorOtpVerificationProvider);
    if (state == null) {
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
    final hasEmail = state.email != null && state.email!.trim().isNotEmpty;
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            ref.read(doctorOtpVerificationProvider.notifier).state = null;
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
              l10n.translate('verificationCodeSent'),
              style: const TextStyle(fontSize: 16, height: 1.4),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _phoneCodeCtrl,
              keyboardType: TextInputType.number,
              maxLength: 6,
              decoration: InputDecoration(
                labelText: l10n.translate('enterPhoneCode'),
                counterText: '',
                border: const OutlineInputBorder(),
              ),
            ),
            if (hasEmail) ...[
              const SizedBox(height: 16),
              TextField(
                controller: _emailCodeCtrl,
                keyboardType: TextInputType.number,
                maxLength: 6,
                decoration: InputDecoration(
                  labelText: l10n.translate('enterEmailCode'),
                  counterText: '',
                  border: const OutlineInputBorder(),
                ),
              ),
            ],
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
