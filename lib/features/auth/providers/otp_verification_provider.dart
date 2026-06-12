import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/registration_provider.dart';

/// Holds pending OTP verification data when navigating to the doctor OTP screen.
/// webConfirmationResult is set on web (signInWithPhoneNumber); verificationId on mobile.
class DoctorOtpVerificationState {
  final String phone;
  final String? email;
  final String verificationId;
  final ConfirmationResult? webConfirmationResult;
  final String doctorFirstName;
  final String doctorLastName;

  DoctorOtpVerificationState({
    required this.phone,
    this.email,
    required this.verificationId,
    this.webConfirmationResult,
    required this.doctorFirstName,
    required this.doctorLastName,
  });
}

final doctorOtpVerificationProvider = StateProvider<DoctorOtpVerificationState?>((ref) => null);

/// Holds pending OTP verification for the new-user registration flow.
class RegisterOtpVerificationState {
  final RegistrationOtpChannel channel;
  final String destination;

  RegisterOtpVerificationState({
    required this.channel,
    required this.destination,
  });
}

final registerOtpVerificationProvider = StateProvider<RegisterOtpVerificationState?>((ref) => null);

/// State for forgot-password flow: identifier → OTP → new password.
class ForgotPasswordFlowState {
  final String identifier;
  final String? email;
  final String? phone;
  final String channel; // 'email' | 'sms'
  final String? verificationId;
  final ConfirmationResult? webConfirmationResult;
  final int? resendToken;
  final String? idToken;
  final String? emailOtpCode;

  ForgotPasswordFlowState({
    required this.identifier,
    this.email,
    this.phone,
    this.channel = 'email',
    this.verificationId,
    this.webConfirmationResult,
    this.resendToken,
    this.idToken,
    this.emailOtpCode,
  });

  ForgotPasswordFlowState copyWith({
    String? identifier,
    String? email,
    String? phone,
    String? channel,
    String? verificationId,
    ConfirmationResult? webConfirmationResult,
    int? resendToken,
    String? idToken,
    String? emailOtpCode,
  }) =>
      ForgotPasswordFlowState(
        identifier: identifier ?? this.identifier,
        email: email ?? this.email,
        phone: phone ?? this.phone,
        channel: channel ?? this.channel,
        verificationId: verificationId ?? this.verificationId,
        webConfirmationResult: webConfirmationResult ?? this.webConfirmationResult,
        resendToken: resendToken ?? this.resendToken,
        idToken: idToken ?? this.idToken,
        emailOtpCode: emailOtpCode ?? this.emailOtpCode,
      );
}

final forgotPasswordFlowProvider = StateProvider<ForgotPasswordFlowState?>((ref) => null);
