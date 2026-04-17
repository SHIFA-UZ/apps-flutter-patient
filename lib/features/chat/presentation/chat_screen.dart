import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/features/chat/domain/chat_models.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/chat_conversation_screen.dart';
import 'package:shifa_patient_app_v1/features/chat/providers/chat_providers.dart';
import 'package:shifa_patient_app_v1/features/chat/data/chat_repository.dart';

class ChatScreen extends ConsumerStatefulWidget {
  const ChatScreen({super.key});

  @override
  ConsumerState<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends ConsumerState<ChatScreen> {
  final _searchController = TextEditingController();
  bool _showSearchResults = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.invalidate(conversationsProvider);
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _startConversation(UserSearchResult user) async {
    final repo = ref.read(chatRepositoryProvider);
    try {
      final conversations = await repo.fetchConversations();
      final existing = conversations.where((c) => c.participantId == user.id).toList();
      if (existing.isNotEmpty) {
        _openConversation(existing.first);
        return;
      }
      // Open conversation so patient can type their own first message
      _openNewConversation(user);
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${l10n.translate('failedToStartChat')}: ${translateError(l10n, e.toString())}')),
        );
      }
    }
  }

  void _openNewConversation(UserSearchResult user) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ChatConversationScreen(
          conversationId: null,
          participantName: user.name,
          participantPhotoUrl: user.photoUrl,
          recipientDoctorId: user.id,
        ),
      ),
    );
  }

  String _localizedLastMessage(String? lastMessage, AppLocalizations l10n) {
    if (lastMessage == null || lastMessage.isEmpty) return l10n.translate('noMessages') ?? '';
    if (lastMessage.trim().toLowerCase() == 'photo') return l10n.translate('photo') ?? lastMessage;
    return lastMessage;
  }

  void _openConversation(ChatContact contact) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ChatConversationScreen(
          conversationId: contact.id,
          participantName: contact.name,
          participantPhotoUrl: contact.photoUrl,
          recipientDoctorId: null,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final conversationsAsync = ref.watch(conversationsProvider);
    final searchQuery = _searchController.text.trim();

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.chat),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              onChanged: (_) {
                setState(() {
                  _showSearchResults = _searchController.text.trim().isNotEmpty;
                });
              },
              decoration: InputDecoration(
                hintText: l10n.searchDoctors,
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
          Expanded(
            child: _showSearchResults && searchQuery.isNotEmpty
                ? _buildSearchResults(searchQuery)
                : _buildConversations(conversationsAsync),
          ),
        ],
      ),
    );
  }

  Widget _buildSearchResults(String query) {
    final l10n = AppLocalizations.of(context)!;
    final searchAsync = ref.watch(userSearchProvider(query));
    return searchAsync.when(
      data: (results) {
        if (results.isEmpty) {
          return Center(child: Text('${l10n.noDoctorsAvailable}'));
        }
        return ListView.separated(
          itemCount: results.length,
          separatorBuilder: (_, __) => const Divider(height: 1),
          itemBuilder: (context, index) {
            final user = results[index];
            return ListTile(
              leading: CircleAvatar(
                backgroundImage: user.photoUrl != null ? NetworkImage(user.photoUrl!) : null,
                child: user.photoUrl == null ? Text(_initials(user.name)) : null,
              ),
              title: Text(user.name),
              subtitle: Text(l10n.doctor),
              trailing: const Icon(Icons.chat_bubble_outline),
              onTap: () => _startConversation(user),
            );
          },
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (err, _) => Center(child: Text('${l10n.error}: $err')),
    );
  }

  Widget _buildConversations(AsyncValue<List<ChatContact>> conversationsAsync) {
    final l10n = AppLocalizations.of(context)!;
    return conversationsAsync.when(
      data: (conversations) {
        if (conversations.isEmpty) {
          return Center(child: Text(l10n.noConversations));
        }
        return RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(conversationsProvider);
          },
          child: ListView.separated(
            padding: EdgeInsets.only(bottom: AppDesignSystem.safeBottomWithNavBar(context)),
            itemCount: conversations.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final conv = conversations[index];
              return ListTile(
                leading: CircleAvatar(
                  backgroundImage: conv.photoUrl != null ? NetworkImage(conv.photoUrl!) : null,
                  child: conv.photoUrl == null ? Text(_initials(conv.name)) : null,
                ),
                title: Text(conv.name),
                subtitle: Text(_localizedLastMessage(conv.lastMessage, l10n)),
                trailing: conv.unread > 0
                    ? CircleAvatar(
                        radius: 10,
                        backgroundColor: const Color(0xFF17C3B2),
                        child: Text(
                          conv.unread.toString(),
                          style: const TextStyle(fontSize: 10, color: Colors.white),
                        ),
                      )
                    : null,
                onTap: () => _openConversation(conv),
              );
            },
          ),
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (err, _) => Center(child: Text('${l10n.error}: $err')),
    );
  }

  String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty || parts.first.isEmpty) return '?';
    if (parts.length == 1) return parts.first.substring(0, 1).toUpperCase();
    return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
  }
}
