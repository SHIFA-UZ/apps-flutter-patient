import 'package:shifa_patient_app_v1/core/models/notification_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';

class NotificationsRepository {
  final ApiClient _apiClient;

  NotificationsRepository(this._apiClient);

  /// Get all notifications for the current patient
  Future<List<NotificationModel>> getNotifications() async {
    try {
      final response = await _apiClient.get('/notifications');
      if (response.data is List) {
        return (response.data as List)
            .map((json) => NotificationModel.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      return [];
    } catch (e) {
      throw Exception('Failed to get notifications: $e');
    }
  }

  /// Get unread notification count
  Future<int> getUnreadCount() async {
    try {
      final response = await _apiClient.get('/notifications/unread/count');
      return (response.data as Map<String, dynamic>)['count'] as int? ?? 0;
    } catch (e) {
      throw Exception('Failed to get unread count: $e');
    }
  }

  /// Mark a notification as read (id can be int or string uuid from FCM payload).
  Future<void> markAsRead(int notificationId) async {
    try {
      await _apiClient.put('/notifications/$notificationId/read');
    } catch (e) {
      throw Exception('Failed to mark notification as read: $e');
    }
  }

  /// Mark a notification as read by string id (e.g. uuid from standardized FCM payload).
  Future<void> markAsReadById(String notificationId) async {
    try {
      await _apiClient.put('/notifications/$notificationId/read');
    } catch (e) {
      throw Exception('Failed to mark notification as read: $e');
    }
  }

  /// Mark all notifications as read
  Future<void> markAllAsRead() async {
    try {
      await _apiClient.put('/notifications/read-all');
    } catch (e) {
      throw Exception('Failed to mark all notifications as read: $e');
    }
  }

  /// Approve a document access request (patient as document owner).
  Future<void> approveDocumentAccessRequest(int requestId) async {
    try {
      await _apiClient.post('/document-access-requests/$requestId/approve', data: <String, dynamic>{});
    } catch (e) {
      throw Exception('Failed to approve access request: $e');
    }
  }

  /// Reject a document access request (patient as document owner).
  Future<void> rejectDocumentAccessRequest(int requestId) async {
    try {
      await _apiClient.post('/document-access-requests/$requestId/reject', data: <String, dynamic>{});
    } catch (e) {
      throw Exception('Failed to reject access request: $e');
    }
  }

  /// Update FCM token for the current patient (for push notifications).
  /// Pass empty string to clear the token on logout.
  Future<void> updateFcmToken(String fcmToken) async {
    try {
      await _apiClient.put('/patients/me/fcm-token', data: <String, dynamic>{'fcmToken': fcmToken});
    } catch (e) {
      throw Exception('Failed to update FCM token: $e');
    }
  }
}
