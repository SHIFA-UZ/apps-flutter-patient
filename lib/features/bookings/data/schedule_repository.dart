import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AvailableSlot {
  final String startAt;
  final String endAt;
  final int slotMinutes;

  AvailableSlot({
    required this.startAt,
    required this.endAt,
    required this.slotMinutes,
  });

  factory AvailableSlot.fromJson(Map<String, dynamic> json) {
    return AvailableSlot(
      startAt: json['startAt'] as String,
      endAt: json['endAt'] as String,
      slotMinutes: json['slotMinutes'] as int,
    );
  }
}

class ScheduleRepository {
  final ApiClient _apiClient;

  ScheduleRepository(this._apiClient);

  /// Get available time slots for a doctor on a specific day
  Future<List<AvailableSlot>> getAvailableSlots({
    required String doctorId,
    required String day, // yyyy-MM-dd
  }) async {
    try {
      final response = await _apiClient.get(
        '/patients/me/schedule/doctors/$doctorId/available',
        queryParameters: {
          'day': day,
        },
      );

      if (response.data is List) {
        return (response.data as List)
            .map((json) => AvailableSlot.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      throw Exception('Failed to get available slots: $e');
    }
  }
}

final scheduleRepositoryProvider = Provider<ScheduleRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ScheduleRepository(apiClient);
});
