import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/features/doctors/data/doctors_search_filters.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class DoctorsListResult {
  final List<DoctorModel> doctors;
  final int totalCount;
  final int? page;
  final int? pageSize;

  const DoctorsListResult({
    required this.doctors,
    required this.totalCount,
    this.page,
    this.pageSize,
  });
}

class DoctorsRepository {
  final ApiClient _apiClient;

  DoctorsRepository(this._apiClient);

  /// Search/list doctors with optional server-side filters.
  Future<DoctorsListResult> searchDoctors([DoctorsSearchFilters filters = const DoctorsSearchFilters()]) async {
    try {
      final response = await _apiClient.get(
        '/public/doctors',
        queryParameters: filters.toQueryParameters(),
      );

      final data = response.data;
      if (data is Map<String, dynamic>) {
        final doctorsJson = data['doctors'];
        final totalCount = (data['totalCount'] as num?)?.toInt() ?? 0;
        final page = (data['page'] as num?)?.toInt();
        final pageSize = (data['pageSize'] as num?)?.toInt();
        if (doctorsJson is List) {
          return DoctorsListResult(
            totalCount: totalCount,
            page: page,
            pageSize: pageSize,
            doctors: doctorsJson
                .map((json) => DoctorModel.fromJson(json as Map<String, dynamic>))
                .toList(),
          );
        }
        return DoctorsListResult(totalCount: totalCount, page: page, pageSize: pageSize, doctors: const []);
      }

      // Backward compatibility if API returns a bare list.
      if (data is List) {
        return DoctorsListResult(
          totalCount: data.length,
          doctors: data
              .map((json) => DoctorModel.fromJson(json as Map<String, dynamic>))
              .toList(),
        );
      }
      return const DoctorsListResult(totalCount: 0, doctors: []);
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
