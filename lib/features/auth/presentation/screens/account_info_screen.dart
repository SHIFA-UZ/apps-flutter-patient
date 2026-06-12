import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/auth/data/auth_repository.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/otp_verification_provider.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/registration_provider.dart';

class AccountInfoScreen extends ConsumerStatefulWidget {
  const AccountInfoScreen({super.key});

  @override
  ConsumerState<AccountInfoScreen> createState() => _AccountInfoScreenState();
}

class _AccountInfoScreenState extends ConsumerState<AccountInfoScreen> {
  final _formKey = GlobalKey<FormState>();
  final _birthDateController = TextEditingController();
  final _genderController = TextEditingController();
  final _addressController = TextEditingController();
  String? _selectedGender;
  bool _isSendingOtp = false;

  @override
  void dispose() {
    _birthDateController.dispose();
    _genderController.dispose();
    _addressController.dispose();
    super.dispose();
  }

  Future<void> _selectDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now().subtract(const Duration(days: 365 * 25)),
      firstDate: DateTime(1900),
      lastDate: DateTime.now(),
    );
    if (picked != null) {
      setState(() {
        // Display format for user
        _birthDateController.text = DateFormat('dd MMMM yyyy').format(picked);
        // Store in backend format (yyyy-MM-dd) in registration provider
        final backendFormat = DateFormat('yyyy-MM-dd').format(picked);
        ref.read(registrationProvider.notifier).updateStep2(
          birthDate: backendFormat,
          gender: _selectedGender ?? '',
          address: _addressController.text,
        );
      });
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
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.accountInformation),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _birthDateController,
                decoration: InputDecoration(
                  labelText: l10n.dateOfBirth,
                  suffixIcon: IconButton(
                    icon: const Icon(Icons.calendar_today),
                    onPressed: _selectDate,
                  ),
                ),
                readOnly: true,
                validator: (value) => value?.isEmpty ?? true ? l10n.translate('required') : null,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                decoration: InputDecoration(
                  labelText: l10n.gender,
                  suffixIcon: Icon(Icons.arrow_drop_down),
                ),
                items: [
                  DropdownMenuItem(value: 'Male', child: Text(l10n.male)),
                  DropdownMenuItem(value: 'Female', child: Text(l10n.female)),
                  DropdownMenuItem(value: 'Other', child: Text(l10n.other)),
                ],
                value: _selectedGender,
                onChanged: (value) {
                  setState(() => _selectedGender = value);
                  if (value != null) {
                    String? birthDateFormatted;
                    if (_birthDateController.text.isNotEmpty) {
                      try {
                        final date = DateFormat('dd MMMM yyyy').parse(_birthDateController.text);
                        birthDateFormatted = DateFormat('yyyy-MM-dd').format(date);
                      } catch (e) {
                        // If parsing fails, leave as null
                      }
                    }
                    ref.read(registrationProvider.notifier).updateStep2(
                      birthDate: birthDateFormatted,
                      gender: value,
                      address: _addressController.text,
                    );
                  }
                },
                validator: (value) => value == null ? l10n.translate('required') : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _addressController,
                decoration: InputDecoration(
                  labelText: '${l10n.address} (${l10n.optional})',
                  hintText: l10n.translate('enterYourAddress'),
                ),
                maxLines: 2,
                onChanged: (value) {
                  String? birthDateFormatted;
                  if (_birthDateController.text.isNotEmpty) {
                    try {
                      final date = DateFormat('dd MMMM yyyy').parse(_birthDateController.text);
                      birthDateFormatted = DateFormat('yyyy-MM-dd').format(date);
                    } catch (e) {
                      // If parsing fails, leave as null
                    }
                  }
                  ref.read(registrationProvider.notifier).updateStep2(
                    birthDate: birthDateFormatted,
                    gender: _selectedGender ?? '',
                    address: value,
                  );
                },
              ),
              const SizedBox(height: 32),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(2, (index) => Container(
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: index == 1 ? const Color(0xFF17C3B2) : Colors.grey[300],
                  ),
                )),
              ),
              const SizedBox(height: 32),
              ShifaPrimaryButton(
                label: l10n.next,
                isLoading: _isSendingOtp,
                onPressed: _isSendingOtp ? null : () async {
                  if (_formKey.currentState!.validate()) {
                    // Ensure registration data is updated
                    final registrationData = ref.read(registrationProvider);
                    if (registrationData.birthDate == null && _birthDateController.text.isNotEmpty) {
                      try {
                        final date = DateFormat('dd MMMM yyyy').parse(_birthDateController.text);
                        ref.read(registrationProvider.notifier).updateStep2(
                          birthDate: DateFormat('yyyy-MM-dd').format(date),
                          gender: _selectedGender ?? '',
                          address: _addressController.text,
                        );
                      } catch (e) {
                        // Date parsing failed, will be caught by validator
                      }
                    }

                    final email = ref.read(registrationProvider).email?.trim();
                    if (email == null || email.isEmpty || !email.contains('@')) {
                      if (!mounted) return;
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(l10n.translate('emailRequired'))),
                      );
                      return;
                    }

                    setState(() => _isSendingOtp = true);
                    try {
                      await ref.read(authRepositoryProvider).sendEmailOtp(email);
                      if (!mounted) return;
                      ref.read(registrationProvider.notifier).setOtpChannel(RegistrationOtpChannel.email);
                      ref.read(registerOtpVerificationProvider.notifier).state = RegisterOtpVerificationState(
                        channel: RegistrationOtpChannel.email,
                        destination: email,
                      );
                      context.push(AppRoutes.registerOtpVerify);
                    } catch (e) {
                      if (!mounted) return;
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(translateError(l10n, e.toString()))),
                      );
                    } finally {
                      if (mounted) setState(() => _isSendingOtp = false);
                    }
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
