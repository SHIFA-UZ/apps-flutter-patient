import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';

class ConfirmDoctorToPatientScreen extends ConsumerStatefulWidget {
  final String phone;
  final String? email;
  final String doctorFirstName;
  final String doctorLastName;

  const ConfirmDoctorToPatientScreen({
    super.key,
    required this.phone,
    this.email,
    required this.doctorFirstName,
    required this.doctorLastName,
  });

  @override
  ConsumerState<ConfirmDoctorToPatientScreen> createState() => _ConfirmDoctorToPatientScreenState();
}

class _ConfirmDoctorToPatientScreenState extends ConsumerState<ConfirmDoctorToPatientScreen> {
  bool _isSending = false;

  Future<void> _onSendCode() async {
    final l10n = AppLocalizations.of(context)!;
    final email = widget.email?.trim();
    if (email == null || email.isEmpty || !email.contains('@')) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('emailRequired'))),
      );
      return;
    }
    setState(() => _isSending = true);
    try {
      final authRepo = ref.read(authRepositoryProvider);
      await authRepo.sendEmailOtp(email);
      if (!mounted) return;
      ref.read(doctorOtpVerificationProvider.notifier).state = DoctorOtpVerificationState(
        phone: widget.phone,
        email: email,
        verificationId: '',
        doctorFirstName: widget.doctorFirstName,
        doctorLastName: widget.doctorLastName,
      );
      context.push(AppRoutes.doctorOtpVerify);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(translateError(l10n, e.toString()))),
        );
      }
    } finally {
      if (mounted) setState(() => _isSending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final doctorName = '${widget.doctorFirstName} ${widget.doctorLastName}'.trim();
    final message = l10n.translate('doctorPatientAccountMessage').replaceAll('{{name}}', doctorName);

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.translate('existingDoctorTitle')),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Icon(Icons.medical_services, size: 64, color: Colors.teal.shade300),
            const SizedBox(height: 16),
            Text(
              message,
              style: const TextStyle(fontSize: 16, height: 1.4),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            ShifaPrimaryButton(
              label: l10n.translate('sendVerificationCode'),
              onPressed: _isSending ? null : _onSendCode,
              isLoading: _isSending,
            ),
          ],
        ),
      ),
    );
  }
}
