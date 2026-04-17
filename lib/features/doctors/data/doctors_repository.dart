import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class DoctorsRepository {
  final ApiClient _apiClient;

  DoctorsRepository(this._apiClient);

  /// Search/list all doctors
  Future<List<DoctorModel>> searchDoctors({
    String? search,
    String? profession,
    String? clinic,
  }) async {
    try {
      final response = await _apiClient.get(
        '/public/doctors',
        queryParameters: {
          if (search != null && search.isNotEmpty) 'search': search,
          if (profession != null && profession.isNotEmpty) 'profession': profession,
          if (clinic != null && clinic.isNotEmpty) 'clinic': clinic,
        },
      );

      if (response.data is List) {
        return (response.data as List)
            .map((json) => DoctorModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      throw Exception('Failed to search doctors: $e');
    }
  }

  /// Get doctor by ID
  Future<DoctorModel> getDoctorById(String doctorId) async {
    try {
      final response = await _apiClient.get('/public/doctors/$doctorId');
      return DoctorModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Failed to get doctor: $e');
    }
  }
}

final doctorsRepositoryProvider = Provider<DoctorsRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DoctorsRepository(apiClient);
});
