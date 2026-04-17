// lib/core/services/daily_video_service.dart
import 'package:dio/dio.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';

class DailyVideoService {
  final ApiClient _apiClient;

  DailyVideoService(this._apiClient);

  /// Get video token from backend
  Future<VideoTokenResponse> getVideoToken({
    required int appointmentId,
    String? roomName,
  }) async {
    try {
      final response = await _apiClient.post(
        '/video/token',
        data: {
          'appointmentId': appointmentId,
          if (roomName != null) 'roomName': roomName,
        },
      );

      if (response.statusCode == 200) {
        return VideoTokenResponse.fromJson(response.data as Map<String, dynamic>);
      } else {
        throw Exception('Failed to get video token: ${response.statusCode}');
      }
    } on DioException catch (e) {
      final message = _serverErrorMessage(e);
      throw Exception(message);
    } catch (e) {
      throw Exception('Failed to get video token: $e');
    }
  }

  /// Extract a user-friendly message from server error response (e.g. 500 with body).
  static String _serverErrorMessage(DioException e) {
    final res = e.response;
    if (res == null) return 'Failed to get video token: ${e.message ?? "No response"}';
    final data = res.data;
    if (data is Map) {
      final msg = data['message'] ?? data['error'] ?? data['reason'];
      if (msg != null && msg.toString().trim().isNotEmpty) {
        return msg.toString();
      }
    }
    if (data != null && data is! Map && data.toString().trim().isNotEmpty) {
      return data.toString();
    }
    final status = res.statusCode;
    final statusMsg = res.statusMessage;
    return 'Server error${status != null ? " ($status)" : ""}${statusMsg != null ? ": $statusMsg" : ""}';
  }
}

class VideoTokenResponse {
  final String token;
  final String roomUrl;
  final String roomName;

  VideoTokenResponse({
    required this.token,
    required this.roomUrl,
    required this.roomName,
  });

  factory VideoTokenResponse.fromJson(Map<String, dynamic> json) {
    return VideoTokenResponse(
      token: json['token'] as String,
      roomUrl: json['roomUrl'] as String,
      roomName: json['roomName'] as String,
    );
  }
}
