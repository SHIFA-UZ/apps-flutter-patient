import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/core/utils/auth_error_sanitizer.dart';
import 'package:shifa_patient_app_v1/core/utils/storage_service.dart';

class LoginResult {
  final String token;
  final bool forcePasswordReset;

  LoginResult({required this.token, required this.forcePasswordReset});
}

class ExistingDoctorInfo {
  final String firstName;
  final String lastName;

  ExistingDoctorInfo({required this.firstName, required this.lastName});
}

/// Result of checking phone/email on create-account: doctor, patient, or none.
class CheckIdentifierResult {
  final String type; // 'doctor' | 'patient' | 'none'
  final String? firstName;
  final String? lastName;

  CheckIdentifierResult({
    required this.type,
    this.firstName,
    this.lastName,
  });

  bool get isDoctor => type == 'doctor';
  bool get isPatient => type == 'patient';
  bool get isNone => type == 'none';
}

class AuthRepository {
  final ApiClient _apiClient;
  final StorageService _storageService;

  AuthRepository(this._apiClient, this._storageService);

  /// Login for the patient app. Sends app=patient so the backend requires PATIENT role.
  /// Doctors who have not created a patient account (one-time flow) will receive 403.
  Future<LoginResult> login(String username, String password) async {
    try {
      final response = await _apiClient.post(
        '/auth/login',
        data: {
          'username': username,
          'password': password,
        },
        queryParameters: {'app': 'patient'},
      );

      if (response.statusCode == 200 && response.data != null) {
        final token = response.data['token'] as String?;
        final forcePasswordReset = response.data['forcePasswordReset'] as bool? ?? false;
        
        if (token != null && token.isNotEmpty) {
          await _storageService.saveAuthToken(token);
          return LoginResult(token: token, forcePasswordReset: forcePasswordReset);
        } else {
          throw Exception('No token received from server');
        }
      } else {
        throw Exception('Invalid response from server: ${response.statusCode}');
      }
    } on DioException catch (e) {
      final statusCode = e.response?.statusCode;
      final data = e.response?.data;
      AppLogger.apiError(statusCode, data, '/auth/login');
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: statusCode,
        data: data ?? e.message,
        defaultMessage: 'Login failed',
      );
      throw Exception(errorMessage);
    } catch (e, stackTrace) {
      AppLogger.error('Login general error:', e, stackTrace);
      throw Exception('Login failed');
    }
  }

  /// Patient app create-account: check if phone/email is already a doctor, patient, or neither.
  /// Backend: POST /auth/check-identifier with { phone?, email? } (at least one required); returns { type, firstName?, lastName? }.
  Future<CheckIdentifierResult> checkIdentifier({
    String? phone,
    String? email,
  }) async {
    final p = phone?.trim();
    final e = email?.trim();
    if ((p == null || p.isEmpty) && (e == null || e.isEmpty)) {
      throw ArgumentError('checkIdentifier requires phone and/or email');
    }
    try {
      final response = await _apiClient.post(
        '/auth/check-identifier',
        data: {
          if (p != null && p.isNotEmpty) 'phone': p,
          if (e != null && e.isNotEmpty) 'email': e,
        },
      );
      final data = response.data as Map<String, dynamic>?;
      if (data == null) return CheckIdentifierResult(type: 'none');
      return CheckIdentifierResult(
        type: (data['type'] as String?) ?? 'none',
        firstName: data['firstName'] as String?,
        lastName: data['lastName'] as String?,
      );
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return CheckIdentifierResult(type: 'none');
      rethrow;
    }
  }

  /// Check if phone/email belongs to an existing doctor. Returns doctor info if found, null otherwise.
  /// Backend: POST /auth/check-existing-doctor with { phone?, email? } (at least one required); returns 200 { firstName, lastName } or 404.
  Future<ExistingDoctorInfo?> checkExistingDoctor({
    String? phone,
    String? email,
  }) async {
    final p = phone?.trim();
    final e = email?.trim();
    if ((p == null || p.isEmpty) && (e == null || e.isEmpty)) {
      throw ArgumentError('checkExistingDoctor requires phone and/or email');
    }
    try {
      final response = await _apiClient.post(
        '/auth/check-existing-doctor',
        data: {
          if (p != null && p.isNotEmpty) 'phone': p,
          if (e != null && e.isNotEmpty) 'email': e,
        },
      );
      final data = response.data as Map<String, dynamic>?;
      if (data == null) return null;
      final firstName = data['firstName'] as String?;
      final lastName = data['lastName'] as String?;
      if (firstName == null || lastName == null) return null;
      return ExistingDoctorInfo(firstName: firstName, lastName: lastName);
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return null;
      final data = e.response?.data;
      if (data is Map && (data['exists'] == false || data['found'] == false)) return null;
      rethrow;
    }
  }

  /// Send 6-digit OTP to email. Backend stores it for verification.
  Future<void> sendEmailOtp(String email) async {
    final response = await _apiClient.post(
      '/auth/send-email-otp',
      data: {'email': email.trim()},
    );
    if (response.statusCode != 200) throw Exception('Failed to send email code');
  }

  /// Create a patient account for an existing doctor. When email is provided, emailOtp must be set (from sendEmailOtp + user entry).
  Future<String> createPatientForDoctor({
    String? phone,
    String? email,
    String? emailOtp,
  }) async {
    try {
      final response = await _apiClient.post(
        '/auth/create-patient-for-doctor',
        data: {
          if (phone != null && phone.trim().isNotEmpty) 'phone': phone.trim(),
          if (email != null && email.trim().isNotEmpty) 'email': email.trim(),
          if (emailOtp != null && emailOtp.trim().isNotEmpty) 'emailOtp': emailOtp.trim(),
        },
      );
      final token = response.data['token'] as String?;
      if (token == null || token.isEmpty) throw Exception('No token received');
      await _storageService.saveAuthToken(token);
      return token;
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data,
        defaultMessage: 'Failed to create patient account',
      );
      throw Exception(errorMessage);
    }
  }

  Future<String> register({
    required String firstName,
    required String lastName,
    String? phone,
    required String email,
    required String password,
    String? birthDate,
    String? gender,
    String? address,
    String? language,
    String? emailOtp,
  }) async {
    try {
      final data = <String, dynamic>{
        'firstName': firstName,
        'lastName': lastName,
        'phone': null,
        'email': email.trim(),
        'password': password,
        'birthDate': birthDate,
        'gender': gender,
        'address': address,
        'language': language,
      };
      final pt = phone?.trim();
      if (pt != null && pt.isNotEmpty) data['phone'] = pt;
      if (emailOtp != null && emailOtp.trim().isNotEmpty) data['emailOtp'] = emailOtp.trim();
      final response = await _apiClient.post('/auth/register-patient', data: data);

      final token = response.data['token'] as String;
      await _storageService.saveAuthToken(token);
      return token;
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data ?? e.message,
        defaultMessage: 'Registration failed',
      );
      throw Exception(errorMessage);
    } catch (e) {
      throw Exception('Registration failed');
    }
  }

  Future<void> logout() async {
    await _storageService.clearAuthToken();
  }

  Future<bool> isAuthenticated() async {
    return await _storageService.isAuthenticated();
  }

  /// Validates if the stored token is still accepted by the backend.
  /// Returns false on 401 (invalid/expired). On network errors, returns true to avoid logging out on connectivity issues.
  Future<bool> validateToken() async {
    try {
      await _apiClient.get('/patients/me/profile');
      return true;
    } on DioException catch (e) {
      if (e.response?.statusCode == 401) return false;
      // Network error, server error, etc - assume valid to avoid logging out on transient issues
      return true;
    } catch (_) {
      return true;
    }
  }

  /// Forced reset (first login): only new password.
  Future<void> resetPassword(String newPassword) async {
    try {
      await _apiClient.post('/auth/reset-password', data: {
        'newPassword': newPassword,
      });
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data,
        defaultMessage: 'Failed to reset password',
      );
      throw Exception(errorMessage);
    }
  }

  /// Forgot password (legacy): after phone OTP verified, send Firebase idToken + new password.
  Future<LoginResult> forgotPasswordReset({
    required String idToken,
    required String newPassword,
  }) async {
    try {
      final response = await _apiClient.post(
        '/auth/forgot-password-reset',
        data: {
          'idToken': idToken,
          'newPassword': newPassword,
        },
      );
      final token = response.data['token'] as String?;
      if (token == null || token.isEmpty) throw Exception('No token received');
      await _storageService.saveAuthToken(token);
      return LoginResult(token: token, forcePasswordReset: false);
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data,
        defaultMessage: 'Failed to reset password',
      );
      throw Exception(errorMessage);
    }
  }

  /// Send forgot-password OTP to email. Identifier can be email or phone.
  Future<void> sendForgotPasswordOtp(String identifier) async {
    try {
      await _apiClient.post(
        '/auth/send-forgot-password-otp',
        data: {
          'identifier': identifier.trim(),
          'app': 'patient',
        },
      );
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data,
        defaultMessage: 'Failed to send verification code',
      );
      throw Exception(errorMessage);
    }
  }

  /// Forgot password with email OTP: verify code + set new password → returns JWT.
  Future<LoginResult> forgotPasswordResetWithEmail({
    required String email,
    required String emailOtp,
    required String newPassword,
  }) async {
    try {
      final response = await _apiClient.post(
        '/auth/forgot-password-reset',
        data: {
          'email': email.trim(),
          'emailOtp': emailOtp.trim(),
          'app': 'patient',
          'newPassword': newPassword,
        },
      );
      final token = response.data['token'] as String?;
      if (token == null || token.isEmpty) throw Exception('No token received');
      await _storageService.saveAuthToken(token);
      return LoginResult(token: token, forcePasswordReset: false);
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data,
        defaultMessage: 'Failed to reset password',
      );
      throw Exception(errorMessage);
    }
  }

  /// Send email OTP for account deletion verification.
  Future<void> sendDeletionOtp(String email) async {
    try {
      await _apiClient.post(
        '/auth/send-email-otp',
        data: {'email': email.trim(), 'purpose': 'ACCOUNT_DELETION'},
      );
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data,
        defaultMessage: 'Failed to send verification code',
      );
      throw Exception(errorMessage);
    }
  }

  /// Change password from settings: current + new password.
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    try {
      await _apiClient.post('/auth/reset-password', data: {
        'currentPassword': currentPassword,
        'newPassword': newPassword,
      });
    } on DioException catch (e) {
      final errorMessage = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data,
        defaultMessage: 'Failed to change password',
      );
      throw Exception(errorMessage);
    }
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AuthRepository(apiClient, StorageService());
});
