import 'package:flutter_riverpod/flutter_riverpod.dart';

/// OTP delivery channel for registration.
enum RegistrationOtpChannel { email, sms }

class RegistrationData {
  final String? firstName;
  final String? lastName;
  final String? phone;
  final String? email;
  final String? password;
  final String? birthDate;
  final String? gender;
  final String? address;
  final String? language;
  final RegistrationOtpChannel? otpChannel;

  RegistrationData({
    this.firstName,
    this.lastName,
    this.phone,
    this.email,
    this.password,
    this.birthDate,
    this.gender,
    this.address,
    this.language,
    this.otpChannel,
  });

  RegistrationData copyWith({
    String? firstName,
    String? lastName,
    String? phone,
    String? email,
    String? password,
    String? birthDate,
    String? gender,
    String? address,
    String? language,
    RegistrationOtpChannel? otpChannel,
  }) {
    return RegistrationData(
      firstName: firstName ?? this.firstName,
      lastName: lastName ?? this.lastName,
      phone: phone ?? this.phone,
      email: email ?? this.email,
      password: password ?? this.password,
      birthDate: birthDate ?? this.birthDate,
      gender: gender ?? this.gender,
      address: address ?? this.address,
      language: language ?? this.language,
      otpChannel: otpChannel ?? this.otpChannel,
    );
  }

  bool get isStep1Complete {
    final hasEmail = email != null && email!.trim().isNotEmpty;
    final hasPhone = phone != null && phone!.trim().isNotEmpty;
    return firstName != null &&
        firstName!.isNotEmpty &&
        lastName != null &&
        lastName!.isNotEmpty &&
        (hasEmail || hasPhone) &&
        password != null &&
        password!.isNotEmpty &&
        otpChannel != null;
  }

  bool get canRegister => isStep1Complete;
}

class RegistrationNotifier extends StateNotifier<RegistrationData> {
  RegistrationNotifier() : super(RegistrationData());

  void updateStep1({
    required String firstName,
    required String lastName,
    String? phone,
    String? email,
    required String password,
    RegistrationOtpChannel? otpChannel,
  }) {
    final trimmedPhone = phone?.trim();
    final newPhone = (trimmedPhone == null || trimmedPhone.isEmpty) ? null : trimmedPhone;
    final trimmedEmail = email?.trim();
    final newEmail = (trimmedEmail == null || trimmedEmail.isEmpty) ? null : trimmedEmail;
    state = RegistrationData(
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phone: newPhone,
      email: newEmail,
      password: password,
      birthDate: state.birthDate,
      gender: state.gender,
      address: state.address,
      language: state.language,
      otpChannel: otpChannel,
    );
  }

  void updateStep2({
    String? birthDate,
    String? gender,
    String? address,
    String? language,
  }) {
    state = state.copyWith(
      birthDate: birthDate,
      gender: gender,
      address: address?.trim(),
      language: language?.trim(),
    );
  }

  void setOtpChannel(RegistrationOtpChannel channel) {
    state = state.copyWith(otpChannel: channel);
  }

  void clear() {
    state = RegistrationData();
  }
}

final registrationProvider =
    StateNotifierProvider<RegistrationNotifier, RegistrationData>((ref) {
  return RegistrationNotifier();
});
