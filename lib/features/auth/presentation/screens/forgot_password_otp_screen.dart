import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';

const _codeExpirySeconds = 10 * 60;
const _resendCooldownSeconds = 60;

class ForgotPasswordOtpScreen extends ConsumerStatefulWidget {
  const ForgotPasswordOtpScreen({super.key});

  @override
  ConsumerState<ForgotPasswordOtpScreen> createState() => _ForgotPasswordOtpScreenState();
}

class _ForgotPasswordOtpScreenState extends ConsumerState<ForgotPasswordOtpScreen> {
  final _codeCtrl = TextEditingController();
  bool _isLoading = false;
  int _codeExpirySecondsRemaining = _codeExpirySeconds;
  int _resendSecondsRemaining = _resendCooldownSeconds;
  Timer? _expiryTimer;
  Timer? _resendTimer;

  @override
  void initState() {
    super.initState();
    _startCountdowns();
  }

  void _startCountdowns() {
    _expiryTimer?.cancel();
    _resendTimer?.cancel();
    _codeExpirySecondsRemaining = _codeExpirySeconds;
    _resendSecondsRemaining = _resendCooldownSeconds;
    _expiryTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() {
        if (_codeExpirySecondsRemaining > 0) _codeExpirySecondsRemaining--;
      });
      if (_codeExpirySecondsRemaining <= 0) _expiryTimer?.cancel();
    });
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
    _expiryTimer?.cancel();
    _resendTimer?.cancel();
    _codeCtrl.dispose();
    super.dispose();
  }

  String _formatTime(int seconds) {
    final m = seconds ~/ 60;
    final s = seconds % 60;
    return '${m}:${s.toString().padLeft(2, '0')}';
  }

  Future<void> _onVerify() async {
    final flowState = ref.read(forgotPasswordFlowProvider);
    if (flowState == null || flowState.email == null) {
      if (mounted) context.go(AppRoutes.forgotPassword);
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
      ref.read(forgotPasswordFlowProvider.notifier).state = flowState.copyWith(emailOtpCode: code);
      if (!mounted) return;
      context.push(AppRoutes.forgotPasswordNewPassword);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(translateError(l10n, e.toString()))),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _onResendCode() async {
    final flowState = ref.read(forgotPasswordFlowProvider);
    if (flowState == null || _resendSecondsRemaining > 0 || _isLoading) return;
    final l10n = AppLocalizations.of(context)!;
    setState(() => _isLoading = true);
    try {
      final authRepo = ref.read(authRepositoryProvider);
      await authRepo.sendForgotPasswordOtp(flowState.identifier);
      if (!mounted) return;
      _startCountdowns();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('codeSentAgain'))),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(translateError(l10n, e.toString()))),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final flowState = ref.watch(forgotPasswordFlowProvider);
    if (flowState == null) {
      return Scaffold(
        body: Center(child: Text(l10n.translate('error'))),
      );
    }
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            ref.read(forgotPasswordFlowProvider.notifier).state = null;
            context.pop();
          },
        ),
        title: Text(l10n.forgotPassword),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 24),
            Text(
              l10n.translate('otpSentToEmail').replaceAll('{email}', flowState.email ?? flowState.identifier),
              style: const TextStyle(fontSize: 16, height: 1.4),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              l10n.translate('codeExpiresIn').replaceAll('{{time}}', _formatTime(_codeExpirySecondsRemaining)),
              style: TextStyle(fontSize: 14, color: _codeExpirySecondsRemaining <= 60 ? Colors.red : Colors.grey[700]),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            TextField(
              controller: _codeCtrl,
              keyboardType: TextInputType.number,
              maxLength: 6,
              decoration: InputDecoration(
                labelText: l10n.translate('enterEmailCode'),
                counterText: '',
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: (_isLoading || _resendSecondsRemaining > 0) ? null : _onResendCode,
              child: Text(
                _resendSecondsRemaining > 0
                    ? l10n.translate('resendCodeIn').replaceAll('{{time}}', _formatTime(_resendSecondsRemaining))
                    : l10n.translate('resendCode'),
              ),
            ),
            const SizedBox(height: 16),
            ShifaPrimaryButton(
              label: l10n.translate('verify'),
              onPressed: _isLoading ? null : _onVerify,
              isLoading: _isLoading,
            ),
          ],
        ),
      ),
    );
  }
}
