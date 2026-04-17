import 'package:equatable/equatable.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';

class ReviewModel extends Equatable {
  final String id;
  final String patientName;
  final String? patientPhotoUrl;
  final int rating;
  final String? comment;
  final String createdAt;

  const ReviewModel({
    required this.id,
    required this.patientName,
    this.patientPhotoUrl,
    required this.rating,
    this.comment,
    required this.createdAt,
  });

  factory ReviewModel.fromJson(Map<String, dynamic> json) {
    return ReviewModel(
      id: json['id']?.toString() ?? '',
      patientName: json['patientName'] ?? 'Anonymous',
      patientPhotoUrl: normalizePhotoUrl(json['patientPhotoUrl'] as String?),
      rating: json['rating'] as int,
      comment: json['comment'] as String?,
      createdAt: json['createdAt'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'patientName': patientName,
      'patientPhotoUrl': patientPhotoUrl,
      'rating': rating,
      'comment': comment,
      'createdAt': createdAt,
    };
  }

  @override
  List<Object?> get props => [id, patientName, patientPhotoUrl, rating, comment, createdAt];
}
