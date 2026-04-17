import 'package:shifa_patient_app_v1/core/models/review_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class ReviewsRepository {
  final ApiClient _apiClient;

  ReviewsRepository(this._apiClient);

  /// Get all reviews for a doctor
  Future<List<ReviewModel>> getDoctorReviews(String doctorId) async {
    try {
      final response = await _apiClient.get('/public/doctors/$doctorId/reviews');

      if (response.data is List) {
        return (response.data as List)
            .map((json) => ReviewModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      throw Exception('Failed to get reviews: $e');
    }
  }

  /// Create a review for a doctor
  Future<ReviewModel> createReview({
    required String doctorId,
    required int rating,
    String? comment,
    String? appointmentId,
  }) async {
    try {
      final response = await _apiClient.post(
        '/public/doctors/$doctorId/reviews',
        data: {
          'rating': rating,
          'comment': comment,
          if (appointmentId != null) 'appointmentId': int.parse(appointmentId),
        },
      );
      return ReviewModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Failed to create review: $e');
    }
  }

  /// Check if a review exists for an appointment
  Future<bool> hasReviewForAppointment(String appointmentId) async {
    try {
      // We'll need to check this on the backend or check all reviews
      // For now, we'll return false and let the backend handle the duplicate check
      return false;
    } catch (e) {
      return false;
    }
  }
}

final reviewsRepositoryProvider = Provider<ReviewsRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ReviewsRepository(apiClient);
});
