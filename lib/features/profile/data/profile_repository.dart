import 'package:dio/dio.dart';
import 'package:shifa_patient_app_v1/core/models/patient_profile_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ProfileRepository {
  final ApiClient _apiClient;

  ProfileRepository(this._apiClient);

  /// Get current patient profile
  Future<PatientProfileModel> getProfile() async {
    try {
      final response = await _apiClient.get('/patients/me/profile');
      return PatientProfileModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ?? 
                            e.response?.data?['error'] ?? 
                            e.message ?? 
                            'Failed to get profile';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to get profile: $e');
    }
  }

  /// Update patient profile
  Future<PatientProfileModel> updateProfile({
    String? firstName,
    String? lastName,
    String? birthDate,
    String? gender,
    String? phone,
    String? address,
    String? language,
    String? photoUrl,
    double? latitude,
    double? longitude,
    String? locationCountry,
    String? locationRegion,
    String? locationDistrict,
    String? locationCity,
    String? locationPostalCode,
    String? locationStreetAddress,
    String? timeZone,
  }) async {
    try {
      final requestData = <String, dynamic>{};
      if (firstName != null) requestData['firstName'] = firstName;
      if (lastName != null) requestData['lastName'] = lastName;
      if (birthDate != null) requestData['birthDate'] = birthDate;
      if (gender != null) requestData['gender'] = gender;
      if (phone != null) requestData['phone'] = phone;
      if (address != null) requestData['address'] = address;
      if (language != null) requestData['language'] = language;
      if (photoUrl != null) requestData['photoUrl'] = photoUrl;
      if (latitude != null) requestData['latitude'] = latitude;
      if (longitude != null) requestData['longitude'] = longitude;
      if (locationCountry != null) requestData['locationCountry'] = locationCountry;
      if (locationRegion != null) requestData['locationRegion'] = locationRegion;
      if (locationDistrict != null) requestData['locationDistrict'] = locationDistrict;
      if (locationCity != null) requestData['locationCity'] = locationCity;
      if (locationPostalCode != null) requestData['locationPostalCode'] = locationPostalCode;
      if (locationStreetAddress != null) requestData['locationStreetAddress'] = locationStreetAddress;
      if (timeZone != null) requestData['timeZone'] = timeZone;

      final response = await _apiClient.patch(
        '/patients/me/profile',
        data: requestData,
      );
      
      return PatientProfileModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ?? 
                            e.response?.data?['error'] ?? 
                            e.message ?? 
                            'Failed to update profile';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to update profile: $e');
    }
  }
}

final profileRepositoryProvider = Provider<ProfileRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ProfileRepository(apiClient);
});
