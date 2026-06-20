import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/phone_input_field.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';

class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _identifierCtrl = TextEditingController();
  bool _isSending = false;

  @override
  void dispose() {
    _identifierCtrl.dispose();
    super.dispose();
  }

  Future<void> _onSendCode() async {
    if (!_formKey.currentState!.validate()) return;
    final raw = _identifierCtrl.text.trim();
    if (raw.isEmpty) return;
    final identifier = raw.contains('@') ? raw.toLowerCase() : normalizePhoneForSms(raw);

    final l10n = AppLocalizations.of(context)!;
    setState(() => _isSending = true);

    try {
      final authRepo = ref.read(authRepositoryProvider);
      final channel = await authRepo.sendForgotPasswordOtp(identifier);
      if (!mounted) return;
      ref.read(forgotPasswordFlowProvider.notifier).state = ForgotPasswordFlowState(
        identifier: identifier,
        email: channel == 'email' ? identifier : null,
        phone: channel == 'sms' ? identifier : null,
        channel: channel,
      );
      context.push(AppRoutes.forgotPasswordOtp);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(userFriendlyError(l10n, e, logContext: 'Forgot password'))),
        );
      }
    } finally {
      if (mounted) setState(() => _isSending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            ref.read(forgotPasswordFlowProvider.notifier).state = null;
            if (Navigator.of(context).canPop()) {
              context.pop();
            } else {
              context.go(AppRoutes.login);
            }
          },
        ),
        title: Text(l10n.forgotPassword),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 24),
              Text(
                l10n.translate('forgotPasswordEnterEmailOrPhone'),
                style: const TextStyle(fontSize: 16, height: 1.4),
              ),
              const SizedBox(height: 24),
              TextFormField(
                controller: _identifierCtrl,
                keyboardType: TextInputType.emailAddress,
                autofillHints: const [AutofillHints.email],
                decoration: InputDecoration(
                  labelText: l10n.translate('emailOrPhone'),
                  prefixIcon: const Icon(Icons.person_outline),
                  border: const OutlineInputBorder(),
                ),
                validator: (value) {
                  final v = (value ?? '').trim();
                  if (v.isEmpty) return l10n.translate('emailOrPhoneRequired');
                  return null;
                },
              ),
              const SizedBox(height: 32),
              ShifaPrimaryButton(
                label: l10n.translate('sendCode'),
                onPressed: _isSending ? null : _onSendCode,
                isLoading: _isSending,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
