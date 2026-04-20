import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AvailableSlot {
  final String startAt;
  final String endAt;
  final int slotMinutes;
  final int? locationId;
  final String? locationLabel;

  AvailableSlot({
    required this.startAt,
    required this.endAt,
    required this.slotMinutes,
    this.locationId,
    this.locationLabel,
  });

  factory AvailableSlot.fromJson(Map<String, dynamic> json) {
    return AvailableSlot(
      startAt: json['startAt'] as String,
      endAt: json['endAt'] as String,
      slotMinutes: json['slotMinutes'] as int,
      locationId: (json['locationId'] as num?)?.toInt(),
      locationLabel: json['locationLabel'] as String?,
    );
  }
}

class ScheduleRepository {
  final ApiClient _apiClient;

  ScheduleRepository(this._apiClient);

  /// Get available time slots for a doctor on a specific day. When [locationId]
  /// is provided, only slots tied to that practice location are returned.
  Future<List<AvailableSlot>> getAvailableSlots({
    required String doctorId,
    required String day, // yyyy-MM-dd
    int? locationId,
  }) async {
    try {
      final response = await _apiClient.get(
        '/patients/me/schedule/doctors/$doctorId/available',
        queryParameters: {
          'day': day,
          if (locationId != null) 'locationId': locationId,
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
