import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/review_model.dart';
import 'package:shifa_patient_app_v1/features/doctors/data/reviews_repository.dart';

class ReviewsState {
  final List<ReviewModel> reviews;
  final bool isLoading;
  final String? error;

  ReviewsState({
    this.reviews = const [],
    this.isLoading = false,
    this.error,
  });

  ReviewsState copyWith({
    List<ReviewModel>? reviews,
    bool? isLoading,
    String? error,
  }) {
    return ReviewsState(
      reviews: reviews ?? this.reviews,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}

class ReviewsNotifier extends StateNotifier<ReviewsState> {
  final ReviewsRepository _repository;

  ReviewsNotifier(this._repository) : super(ReviewsState());

  Future<void> loadReviews(String doctorId) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final reviews = await _repository.getDoctorReviews(doctorId);
      state = state.copyWith(reviews: reviews, isLoading: false);
    } catch (e) {
      state = state.copyWith(
        error: e.toString(),
        isLoading: false,
      );
    }
  }

  Future<void> createReview({
    required String doctorId,
    required int rating,
    String? comment,
    String? appointmentId,
  }) async {
    try {
      await _repository.createReview(
        doctorId: doctorId,
        rating: rating,
        comment: comment,
        appointmentId: appointmentId,
      );
      // Reload reviews after creating
      await loadReviews(doctorId);
    } catch (e) {
      throw Exception('Failed to create review: $e');
    }
  }
}

final reviewsProvider = StateNotifierProvider.family<ReviewsNotifier, ReviewsState, String>((ref, doctorId) {
  final repository = ref.watch(reviewsRepositoryProvider);
  return ReviewsNotifier(repository);
});
