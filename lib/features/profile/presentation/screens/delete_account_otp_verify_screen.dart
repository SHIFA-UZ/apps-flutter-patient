import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/data/account_deletion_repository.dart';

const _resendCooldownSeconds = 5 * 60;

class DeleteAccountOtpVerifyScreen extends ConsumerStatefulWidget {
  final String challengeId;
  final String? email;

  const DeleteAccountOtpVerifyScreen({
    super.key,
    required this.challengeId,
    this.email,
  });

  @override
  ConsumerState<DeleteAccountOtpVerifyScreen> createState() =>
      _DeleteAccountOtpVerifyScreenState();
}

class _DeleteAccountOtpVerifyScreenState
    extends ConsumerState<DeleteAccountOtpVerifyScreen> {
  final _codeCtrl = TextEditingController();
  bool _isLoading = false;
  int _resendSecondsRemaining = _resendCooldownSeconds;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _startTimer();
    Future.microtask(_sendCode);
  }

  void _startTimer() {
    _timer?.cancel();
    _resendSecondsRemaining = _resendCooldownSeconds;
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      setState(() {
        if (_resendSecondsRemaining > 0) _resendSecondsRemaining--;
      });
      if (_resendSecondsRemaining <= 0) _timer?.cancel();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    final l10n = AppLocalizations.of(context)!;
    final email = widget.email?.trim();
    if (email == null || email.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('emailRequired'))),
      );
      return;
    }
    try {
      AppLogger.debug('[DeleteAccount] sendDeletionOtp -> /auth/send-email-otp');
      final authRepo = ref.read(authRepositoryProvider);
      await authRepo.sendDeletionOtp(email).timeout(
        const Duration(seconds: 10),
        onTimeout: () => throw Exception(
          l10n.translate('requestTimeout'),
        ),
      );
      AppLogger.debug('[DeleteAccount] sendDeletionOtp <- success');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('verificationCodeSent'))),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(userFriendlyError(l10n, e, logContext: 'DeleteAccount OTP send')),
        ),
      );
    }
  }

  Future<void> _onVerifyAndDelete() async {
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
      final repo = ref.read(accountDeletionRepositoryProvider);
      await repo.confirmDeletion(
        challengeId: widget.challengeId,
        email: widget.email!.trim(),
        emailOtp: code,
      ).timeout(
        const Duration(seconds: 10),
        onTimeout: () => throw Exception(
          l10n.translate('requestTimeout'),
        ),
      );

      await ref.read(authStateProvider.notifier).logout();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('accountDeletedSuccess'))),
      );
      context.go(AppRoutes.login);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(userFriendlyError(l10n, e, logContext: 'DeleteAccount confirm')),
        ),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final mm = (_resendSecondsRemaining / 60).floor().toString().padLeft(2, '0');
    final ss = (_resendSecondsRemaining % 60).toString().padLeft(2, '0');
    final canResend = _resendSecondsRemaining <= 0 && !_isLoading;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.translate('deleteAccountVerifyTitle'))),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(l10n.translate('deleteAccountOtpSubtitle')),
            const SizedBox(height: 16),
            TextField(
              controller: _codeCtrl,
              keyboardType: TextInputType.number,
              textInputAction: TextInputAction.done,
              decoration: InputDecoration(
                labelText: l10n.translate('verificationCode'),
              ),
              maxLength: 6,
            ),
            const SizedBox(height: 8),
            ShifaPrimaryButton(
              label: l10n.translate('confirmDeletion'),
              onPressed: _isLoading ? null : _onVerifyAndDelete,
              isLoading: _isLoading,
              destructive: true,
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: canResend
                  ? () {
                      _startTimer();
                      _sendCode();
                    }
                  : null,
              child: Text(
                canResend
                    ? l10n.translate('resendCode')
                    : l10n.translate('resendCodeIn').replaceAll('{{time}}', '$mm:$ss'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
