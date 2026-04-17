import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/core/utils/auth_error_sanitizer.dart';

class DeleteAccountRequestResult {
  final String challengeId;
  final String? maskedEmail;
  final int expiresInSeconds;

  DeleteAccountRequestResult({
    required this.challengeId,
    this.maskedEmail,
    this.expiresInSeconds = 300,
  });

  factory DeleteAccountRequestResult.fromJson(Map<String, dynamic> json) {
    return DeleteAccountRequestResult(
      challengeId: (json['challengeId'] as String?) ?? '',
      maskedEmail: json['maskedEmail'] as String?,
      expiresInSeconds: (json['expiresInSeconds'] as num?)?.toInt() ?? 300,
    );
  }
}

class AccountDeletionRepository {
  final ApiClient _api;

  AccountDeletionRepository(this._api);

  Future<DeleteAccountRequestResult> requestDeletion() async {
    try {
      AppLogger.debug('[DeleteAccount] requestDeletion -> POST /patients/me/delete-account/request');
      final res = await _api.post('/patients/me/delete-account/request');
      final data = (res.data as Map).cast<String, dynamic>();
      final out = DeleteAccountRequestResult.fromJson(data);
      if (out.challengeId.isEmpty)
        throw Exception('Invalid response from server');
      AppLogger.debug('[DeleteAccount] requestDeletion <- success challenge received');
      return out;
    } on DioException catch (e) {
      AppLogger.error(
        '[DeleteAccount] requestDeletion failed:',
        'type=${e.type} status=${e.response?.statusCode} path=${e.requestOptions.path}',
      );
      final msg = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data ?? e.message,
        defaultMessage: 'Failed to request account deletion',
      );
      throw Exception(msg);
    }
  }

  Future<void> confirmDeletion({
    required String challengeId,
    required String email,
    required String emailOtp,
  }) async {
    try {
      AppLogger.debug('[DeleteAccount] confirmDeletion -> POST /patients/me/delete-account/confirm');
      await _api.post(
        '/patients/me/delete-account/confirm',
        data: {
          'challengeId': challengeId,
          'email': email,
          'emailOtp': emailOtp,
        },
      );
      AppLogger.debug('[DeleteAccount] confirmDeletion <- success');
    } on DioException catch (e) {
      AppLogger.error(
        '[DeleteAccount] confirmDeletion failed:',
        'type=${e.type} status=${e.response?.statusCode} path=${e.requestOptions.path}',
      );
      final msg = AuthErrorSanitizer.sanitize(
        statusCode: e.response?.statusCode,
        data: e.response?.data ?? e.message,
        defaultMessage: 'Failed to delete account',
      );
      throw Exception(msg);
    }
  }
}

final accountDeletionRepositoryProvider = Provider<AccountDeletionRepository>((
  ref,
) {
  final api = ref.watch(apiClientProvider);
  return AccountDeletionRepository(api);
});
