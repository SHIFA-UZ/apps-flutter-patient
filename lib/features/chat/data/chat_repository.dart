import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/features/chat/domain/chat_models.dart';

class ChatRepository {
  final ApiClient _apiClient;

  ChatRepository(this._apiClient);

  Future<List<ChatContact>> fetchConversations() async {
    try {
      final response = await _apiClient.get('/patients/me/messages/conversations');
      final data = response.data as List<dynamic>;
      return data.map((e) => ChatContact.fromJson(e as Map<String, dynamic>)).toList();
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to fetch conversations';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to fetch conversations: $e');
    }
  }

  Future<ConversationWithMessages> fetchConversation(String conversationId) async {
    try {
      final response = await _apiClient.get('/patients/me/messages/conversations/$conversationId');
      return ConversationWithMessages.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to fetch conversation';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to fetch conversation: $e');
    }
  }

  Future<ChatMessage> sendMessage({
    String? conversationId,
    String? recipientDoctorId,
    String? text,
    String? type, // "text", "image", "voice", "document"
    String? attachmentUrl,
    String? attachmentName,
    String? thumbnailUrl,
    int? fileSize,
    int? duration, // For voice messages in seconds
  }) async {
    try {
      final payload = <String, dynamic>{
        if (conversationId != null) 'conversationId': int.parse(conversationId),
        if (recipientDoctorId != null) 'recipientDoctorId': int.parse(recipientDoctorId),
        if (text != null) 'text': text,
        if (type != null) 'type': type,
        if (attachmentUrl != null) 'attachmentUrl': attachmentUrl,
        if (attachmentName != null) 'attachmentName': attachmentName,
        if (thumbnailUrl != null) 'thumbnailUrl': thumbnailUrl,
        if (fileSize != null) 'fileSize': fileSize,
        if (duration != null) 'duration': duration,
      };
      final response = await _apiClient.post('/patients/me/messages/send', data: payload);
      return ChatMessage.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to send message';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to send message: $e');
    }
  }

  Future<void> markConversationAsRead(String conversationId) async {
    try {
      await _apiClient.post('/patients/me/messages/conversations/$conversationId/read');
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to mark as read';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to mark as read: $e');
    }
  }

  Future<List<UserSearchResult>> searchUsers(String query) async {
    try {
      final response = await _apiClient.get(
        '/patients/me/messages/search',
        queryParameters: {'q': query},
      );
      final data = response.data as List<dynamic>;
      return data.map((e) => UserSearchResult.fromJson(e as Map<String, dynamic>)).toList();
    } catch (e) {
      if (e is DioException) {
        final errorMessage = e.response?.data?['message'] ??
            e.response?.data?['error'] ??
            e.message ??
            'Failed to search users';
        throw Exception(errorMessage);
      }
      throw Exception('Failed to search users: $e');
    }
  }

  Future<int> getUnreadCount() async {
    try {
      final response = await _apiClient.get('/patients/me/messages/unread-count');
      final data = response.data as Map<String, dynamic>;
      return (data['count'] as int?) ?? 0;
    } catch (e) {
      return 0;
    }
  }
}

final chatRepositoryProvider = Provider<ChatRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ChatRepository(apiClient);
});
