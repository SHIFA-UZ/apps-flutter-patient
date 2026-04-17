import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/features/chat/data/chat_repository.dart';
import 'package:shifa_patient_app_v1/features/chat/domain/chat_models.dart';

final conversationsProvider = FutureProvider<List<ChatContact>>((ref) async {
  final repo = ref.read(chatRepositoryProvider);
  return repo.fetchConversations();
});

final conversationProvider =
    FutureProvider.family<ConversationWithMessages, String>((ref, conversationId) async {
  // New conversation (no backend conversation yet): return empty so patient can type first message.
  if (conversationId.isEmpty || conversationId.startsWith('new_')) {
    return ConversationWithMessages(
      conversation: ChatContact(
        id: conversationId,
        name: '',
        participantId: '',
        isDoctor: true,
        lastMessage: null,
        lastActivity: null,
        unread: 0,
        messages: [],
      ),
      messages: [],
    );
  }
  final repo = ref.read(chatRepositoryProvider);
  return repo.fetchConversation(conversationId);
});

/// Optimistically added messages (e.g. immediately after send) until refetch includes them.
final optimisticMessagesProvider =
    StateProvider.family<List<ChatMessage>, String>((ref, conversationId) => []);

final userSearchProvider = FutureProvider.family<List<UserSearchResult>, String>((ref, query) async {
  if (query.trim().isEmpty) return [];
  final repo = ref.read(chatRepositoryProvider);
  return repo.searchUsers(query);
});

final unreadCountProvider = StreamProvider<int>((ref) async* {
  final repo = ref.read(chatRepositoryProvider);
  while (true) {
    try {
      final count = await repo.getUnreadCount();
      yield count;
    } catch (_) {
      yield 0;
    }
    await Future.delayed(const Duration(seconds: 10));
  }
});
