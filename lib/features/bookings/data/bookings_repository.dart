import 'package:dio/dio.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/core/models/appointment_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class BookingsRepository {
  final ApiClient _apiClient;

  BookingsRepository(this._apiClient);

  /// Get all appointments for the current patient
  Future<List<AppointmentModel>> getAppointments({
    String? status,
    String? startDate,
    String? endDate,
  }) async {
    try {
      final response = await _apiClient.get(
        '/patients/me/appointments',
        queryParameters: {
          if (status != null) 'status': status,
          if (startDate != null) 'startDate': startDate,
          if (endDate != null) 'endDate': endDate,
        },
      );

      if (response.data is List) {
        return (response.data as List)
            .map((json) => AppointmentModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      throw Exception('Failed to get appointments: $e');
    }
  }

  /// Get appointment by ID
  Future<AppointmentModel> getAppointmentById(String appointmentId) async {
    try {
      final response = await _apiClient.get('/patients/me/appointments/$appointmentId');
      return AppointmentModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Failed to get appointment: $e');
    }
  }

  /// Book a new appointment (Shifa Global Time Architecture v2: startAt is ISO 8601 UTC).
  /// [locationId] is required when the chosen doctor has multiple practice
  /// locations and the booking isn't a video consultation.
  Future<AppointmentModel> bookAppointment({
    required String doctorId,
    required String startAt, // ISO 8601 UTC e.g. 2026-02-12T13:00:00Z
    int slotMinutes = 30,
    String? reason,
    bool isVideo = false,
    int? locationId,
  }) async {
    try {
      final requestData = {
        'doctorId': int.parse(doctorId),
        'startAt': startAt,
        'slotMinutes': slotMinutes,
        if (reason != null && reason.isNotEmpty) 'reason': reason,
        'isVideo': isVideo,
        if (locationId != null) 'locationId': locationId,
      };
      
      AppLogger.debug('Booking request data: $requestData');
      
      final response = await _apiClient.post(
        '/patients/me/appointments',
        data: requestData,
      );
      
      AppLogger.debug('Booking response: ${response.data}');
      
      return AppointmentModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      AppLogger.error('Booking repository error:', e);
      if (e is DioException) {
        AppLogger.apiError(e.response?.statusCode, e.response?.data, '/patients/me/appointments');
        final errorMessage = e.response?.data?['message'] ?? 
                            e.response?.data?['error'] ?? 
                            e.message ?? 
                            'Failed to book appointment';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to book appointment: $e');
    }
  }

  /// Cancel an appointment
  Future<void> cancelAppointment(String appointmentId) async {
    try {
      await _apiClient.delete('/patients/me/appointments/$appointmentId');
    } catch (e) {
      throw Exception('Failed to cancel appointment: $e');
    }
  }

  /// Get appointment summary for signing screen (doctor name, date, etc.)
  Future<Map<String, dynamic>> getAppointmentSummary(String appointmentId) async {
    try {
      final response = await _apiClient.get('/patients/me/appointments/$appointmentId');
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Failed to load appointment: $e');
    }
  }

  /// Submit patient signature for an appointment
  Future<void> submitSignature(String appointmentId, String signatureImageBase64) async {
    try {
      await _apiClient.post(
        '/patients/me/appointments/$appointmentId/submit-signature',
        data: {'signatureImageBase64': signatureImageBase64},
      );
    } catch (e) {
      throw Exception('Failed to submit signature: $e');
    }
  }

  /// Get the current patient's review for an appointment, if any. Returns null when 404.
  Future<AppointmentReview?> getReviewForAppointment(String appointmentId) async {
    try {
      final response = await _apiClient.get('/patients/me/appointments/$appointmentId/review');
      final data = response.data as Map<String, dynamic>;
      return AppointmentReview(
        rating: data['rating'] as int,
        comment: data['comment'] as String?,
        createdAt: data['createdAt'] as String? ?? '',
      );
    } catch (e) {
      if (e is DioException && e.response?.statusCode == 404) return null;
      return null;
    }
  }

  Future<void> generateVisitSummary(String appointmentId, {String? language, bool force = false}) async {
    await _apiClient.post(
      '/patients/me/appointments/$appointmentId/ai-visit-summary:generate',
      data: {
        if (language != null && language.isNotEmpty) 'language': language,
        'force': force,
      },
    );
  }

  Future<VisitSummaryResponse> getVisitSummary(String appointmentId, {String? language}) async {
    final response = await _apiClient.get(
      '/patients/me/appointments/$appointmentId/ai-visit-summary',
      queryParameters: {
        if (language != null && language.isNotEmpty) 'language': language,
      },
    );
    return VisitSummaryResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<VisitSummaryAskResponse> askVisitSummary(
    String appointmentId, {
    required String question,
    String? language,
  }) async {
    final response = await _apiClient.post(
      '/patients/me/appointments/$appointmentId/ai-visit-summary/ask',
      data: {
        if (language != null && language.isNotEmpty) 'language': language,
        'question': question,
      },
    );
    final data = response.data as Map<String, dynamic>;
    return VisitSummaryAskResponse.fromJson(data);
  }
}

/// Minimal model for "my review" for an appointment (rating already submitted).
class AppointmentReview {
  final int rating;
  final String? comment;
  final String createdAt;

  AppointmentReview({required this.rating, this.comment, required this.createdAt});
}

class VisitSummaryResponse {
  final String status;
  final String language;
  final int version;
  final String? generatedAt;
  final Map<String, dynamic>? content;

  VisitSummaryResponse({
    required this.status,
    required this.language,
    required this.version,
    this.generatedAt,
    this.content,
  });

  factory VisitSummaryResponse.fromJson(Map<String, dynamic> json) {
    return VisitSummaryResponse(
      status: (json['status'] as String?) ?? 'not_ready',
      language: (json['language'] as String?) ?? 'en',
      version: (json['version'] as num?)?.toInt() ?? 1,
      generatedAt: json['generatedAt'] as String?,
      content: json['content'] is Map<String, dynamic>
          ? json['content'] as Map<String, dynamic>
          : null,
    );
  }
}

class VisitSummaryAskResponse {
  final String answer;
  final List<String> citations;

  VisitSummaryAskResponse({
    required this.answer,
    this.citations = const [],
  });

  factory VisitSummaryAskResponse.fromJson(Map<String, dynamic> json) {
    final raw = json['citations'];
    final citations = raw is List
        ? raw.map((e) => e.toString()).where((e) => e.isNotEmpty).toList()
        : <String>[];
    return VisitSummaryAskResponse(
      answer: json['answer']?.toString() ?? '',
      citations: citations,
    );
  }
}

final bookingsRepositoryProvider = Provider<BookingsRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return BookingsRepository(apiClient);
});
