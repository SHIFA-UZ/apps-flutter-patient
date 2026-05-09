import 'package:flutter_riverpod/flutter_riverpod.dart';

class RegistrationData {
  final String? firstName;
  final String? lastName;
  final String? phone;
  final String? email;
  final String? password;
  final String? birthDate; // yyyy-MM-dd format
  final String? gender;
  final String? address;
  final String? language;

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
    );
  }

  bool get isStep1Complete =>
      firstName != null &&
      firstName!.isNotEmpty &&
      lastName != null &&
      lastName!.isNotEmpty &&
      email != null &&
      email!.trim().isNotEmpty &&
      password != null &&
      password!.isNotEmpty;

  bool get isStep2Complete =>
      birthDate != null && birthDate!.isNotEmpty && gender != null && gender!.isNotEmpty;

  bool get canRegister => isStep1Complete && isStep2Complete;
}

class RegistrationNotifier extends StateNotifier<RegistrationData> {
  RegistrationNotifier() : super(RegistrationData());

  void updateStep1({
    required String firstName,
    required String lastName,
    String? phone,
    required String email,
    required String password,
  }) {
    final trimmedPhone = phone?.trim();
    final newPhone = (trimmedPhone == null || trimmedPhone.isEmpty) ? null : trimmedPhone;
    state = RegistrationData(
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phone: newPhone,
      email: email.trim(),
      password: password,
      birthDate: state.birthDate,
      gender: state.gender,
      address: state.address,
      language: state.language,
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

  void clear() {
    state = RegistrationData();
  }
}

final registrationProvider =
    StateNotifierProvider<RegistrationNotifier, RegistrationData>((ref) {
  return RegistrationNotifier();
});
