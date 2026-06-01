import 'dart:async';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_linkify/flutter_linkify.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:image_picker/image_picker.dart';
import 'package:dio/dio.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/utils/permission_rationale.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/features/chat/domain/chat_models.dart';
import 'package:shifa_patient_app_v1/features/chat/providers/chat_providers.dart';
import 'package:shifa_patient_app_v1/features/chat/data/chat_repository.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/widgets/text_message_bubble.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/widgets/image_message_bubble.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/widgets/voice_message_bubble.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/widgets/document_message_bubble.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/widgets/voice_recording_dialog.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';
import 'package:shifa_patient_app_v1/core/utils/voice_file_exists.dart';

class ChatConversationScreen extends ConsumerStatefulWidget {
  /// Existing conversation id, or null for a new conversation (patient types first message).
  final String? conversationId;
  final String participantName;
  final String? participantPhotoUrl;
  /// When starting a new conversation, pass the doctor id so the first message creates the chat.
  final String? recipientDoctorId;

  const ChatConversationScreen({
    super.key,
    this.conversationId,
    required this.participantName,
    this.participantPhotoUrl,
    this.recipientDoctorId,
  });

  @override
  ConsumerState<ChatConversationScreen> createState() => _ChatConversationScreenState();
}

class _ChatConversationScreenState extends ConsumerState<ChatConversationScreen> {
  final TextEditingController _messageCtrl = TextEditingController();
  final ScrollController _scrollCtrl = ScrollController();
  Timer? _refreshTimer;
  bool _isUploadingImage = false;
  bool _isUploadingVoice = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _markAsRead();
      _refreshConversation();
    });
    _refreshTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _refreshConversation();
    });
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _messageCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  String get _effectiveConversationId =>
      widget.conversationId ?? 'new_${widget.recipientDoctorId ?? ''}';

  Future<void> _refreshConversation() async {
    ref.invalidate(conversationProvider(_effectiveConversationId));
  }

  Future<void> _markAsRead() async {
    if (_effectiveConversationId.startsWith('new_')) return;
    final repo = ref.read(chatRepositoryProvider);
    try {
      await repo.markConversationAsRead(_effectiveConversationId);
      ref.invalidate(conversationsProvider);
    } catch (_) {}
  }

  Future<void> _sendMessage({String? imageUrl, String? imageName}) async {
    final text = _messageCtrl.text.trim();
    if (text.isEmpty && imageUrl == null) return;
    final isNewConversation = widget.conversationId == null || _effectiveConversationId.startsWith('new_');
    final repo = ref.read(chatRepositoryProvider);
    try {
      final sentMessage = await repo.sendMessage(
        conversationId: isNewConversation ? null : widget.conversationId,
        recipientDoctorId: isNewConversation ? widget.recipientDoctorId : null,
        text: text.isEmpty ? null : text,
        type: imageUrl != null ? 'image' : null,
        attachmentUrl: imageUrl,
        attachmentName: imageName,
      );
      _messageCtrl.clear();
      ref.read(optimisticMessagesProvider(_effectiveConversationId).notifier).update((list) => [...list, sentMessage]);
      _refreshConversation();
      ref.invalidate(conversationsProvider);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollCtrl.hasClients) {
          _scrollCtrl.animateTo(
            _scrollCtrl.position.maxScrollExtent + 80,
            duration: const Duration(milliseconds: 250),
            curve: Curves.easeOut,
          );
        }
      });
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${l10n.translate('failedToSend')}: ${userFriendlyError(l10n, e, logContext: 'Send message')}')),
        );
      }
    }
  }

  Future<void> _pickAndSendImage() async {
    final l10n = AppLocalizations.of(context)!;
    final patientId = ref.read(profileProvider).profile?.id;
    if (patientId == null || patientId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${l10n.profile} ${l10n.error.toLowerCase()}')),
      );
      return;
    }

    // Show bottom sheet to choose camera or gallery
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.camera_alt, color: Color(0xFF17C3B2)),
              title: Text(l10n.takePhoto ?? 'Take Photo'),
              onTap: () => Navigator.pop(context, ImageSource.camera),
            ),
            ListTile(
              leading: const Icon(Icons.photo_library, color: Color(0xFF17C3B2)),
              title: Text(l10n.chooseFromGallery ?? 'Choose from Gallery'),
              onTap: () => Navigator.pop(context, ImageSource.gallery),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );

    if (source == null) return;

    if (source == ImageSource.camera) {
      await showPermissionRationale(
        context: context,
        rationaleKey: 'permissionRationaleCamera',
      );
      if (!mounted) return;
    }

    ref.read(appLockTemporaryDisableProvider.notifier).disable();
    setState(() {
      _isUploadingImage = true;
    });

    try {
      final picker = ImagePicker();
      final image = await picker.pickImage(
        source: source,
        imageQuality: 85,
      );

      if (image == null) {
        ref.read(appLockTemporaryDisableProvider.notifier).enable();
        setState(() {
          _isUploadingImage = false;
        });
        return;
      }

      final bytes = await image.readAsBytes();
      final fileName = 'chat_${DateTime.now().millisecondsSinceEpoch}.jpg';

      // Upload via chat attachment endpoint so each image gets a unique URL (UUID filename).
      // Using /patients/me/documents with same title overwrote the same file and sent the first image again.
      final apiClient = ref.read(apiClientProvider);
      final formData = FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: fileName),
      });
      final response = await apiClient.dio.post(
        '/messages/upload-attachment',
        data: formData,
      );

      if (response.statusCode == null || response.statusCode! < 200 || response.statusCode! >= 300) {
        throw Exception('Upload failed: ${response.statusCode}');
      }
      final fileUrl = (response.data as Map<String, dynamic>)['url'] as String?;
      if (fileUrl == null || fileUrl.isEmpty) {
        throw Exception('No URL in upload response');
      }

      await _sendMessage(
        imageUrl: fileUrl,
        imageName: fileName,
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${l10n.translate('failedToUploadImage')}: ${userFriendlyError(l10n, e, logContext: 'Image upload')}',
            ),
          ),
        );
      }
    } finally {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
      if (mounted) {
        setState(() {
          _isUploadingImage = false;
        });
      }
    }
  }

  Future<void> _showVoiceRecordingDialog() async {
    await showPermissionRationale(
      context: context,
      rationaleKey: 'permissionRationaleMicrophone',
    );
    if (!mounted) return;
    await showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => VoiceRecordingDialog(
        onRecordingComplete: (filePath, duration) async {
          Navigator.pop(ctx);
          await _sendVoiceMessage(filePath, duration);
        },
        onCancel: () => Navigator.pop(ctx),
      ),
    );
  }

  Future<void> _sendVoiceMessage(String filePath, int durationSeconds) async {
    final l10n = AppLocalizations.of(context)!;
    ref.read(appLockTemporaryDisableProvider.notifier).disable();
    setState(() => _isUploadingVoice = true);
    try {
      if (!kIsWeb) {
        final exists = await voiceFileExists(filePath);
        if (!exists) {
          ref.read(appLockTemporaryDisableProvider.notifier).enable();
          return;
        }
      }
      final bytes = await XFile(filePath).readAsBytes();
      final fileSize = bytes.length;
      final fileName = 'voice_${DateTime.now().millisecondsSinceEpoch}.m4a';

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.translate('uploadingFile'))),
      );

      final apiClient = ref.read(apiClientProvider);
      final formData = FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: fileName),
      });
      final response = await apiClient.dio.post(
        '/messages/upload-attachment',
        data: formData,
      );

      if (response.statusCode == null || response.statusCode! < 200 || response.statusCode! >= 300) {
        throw Exception('Upload failed: ${response.statusCode}');
      }
      final fileUrl = (response.data as Map<String, dynamic>)['url'] as String?;
      if (fileUrl == null || fileUrl.isEmpty) {
        throw Exception('No URL in upload response');
      }

      if (!mounted) return;
      ScaffoldMessenger.of(context).hideCurrentSnackBar();

      final isNewConversation = widget.conversationId == null || _effectiveConversationId.startsWith('new_');
      final repo = ref.read(chatRepositoryProvider);
      await repo.sendMessage(
        conversationId: isNewConversation ? null : widget.conversationId,
        recipientDoctorId: isNewConversation ? widget.recipientDoctorId : null,
        text: null,
        type: 'voice',
        attachmentUrl: fileUrl,
        attachmentName: fileName,
        fileSize: fileSize,
        duration: durationSeconds,
      );

      _refreshConversation();
      ref.invalidate(conversationsProvider);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollCtrl.hasClients) {
          _scrollCtrl.animateTo(
            _scrollCtrl.position.maxScrollExtent + 80,
            duration: const Duration(milliseconds: 250),
            curve: Curves.easeOut,
          );
        }
      });
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.translate('errorRecordingVoice')}: ${userFriendlyError(l10n, e, logContext: 'Voice upload')}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      ref.read(appLockTemporaryDisableProvider.notifier).enable();
      if (mounted) {
        setState(() => _isUploadingVoice = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final conversationAsync = ref.watch(conversationProvider(_effectiveConversationId));

    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            CircleAvatar(
              radius: 16,
              backgroundImage: widget.participantPhotoUrl != null
                  ? NetworkImage(normalizePhotoUrl(widget.participantPhotoUrl!) ?? widget.participantPhotoUrl!)
                  : null,
              child: widget.participantPhotoUrl == null
                  ? Text(_initials(widget.participantName))
                  : null,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                widget.participantName,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
      body: conversationAsync.when(
        data: (data) {
          ref.listen(conversationProvider(_effectiveConversationId), (prev, next) {
            next.whenData((d) {
              final ids = d.messages.map((m) => m.id).toSet();
              ref.read(optimisticMessagesProvider(_effectiveConversationId).notifier).state =
                  ref.read(optimisticMessagesProvider(_effectiveConversationId)).where((o) => !ids.contains(o.id)).toList();
            });
          });
          final optimistic = ref.watch(optimisticMessagesProvider(_effectiveConversationId));
          final existingIds = data.messages.map((m) => m.id).toSet();
          final merged = [
            ...data.messages,
            ...optimistic.where((o) => !existingIds.contains(o.id)),
          ]..sort((a, b) => a.sentAt.compareTo(b.sentAt));
          return Column(
          children: [
            Expanded(
              child: RefreshIndicator(
                onRefresh: _refreshConversation,
                child: ListView.builder(
                  controller: _scrollCtrl,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                  itemCount: merged.length,
                  itemBuilder: (context, index) {
                    final msg = merged[index];
                    return _buildMessageBubble(msg);
                  },
                ),
              ),
            ),
            SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(8, 8, 8, 12),
                child: Row(
                  children: [
                    IconButton(
                      icon: _isUploadingImage
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.photo_camera, color: Color(0xFF17C3B2)),
                      onPressed: _isUploadingImage ? null : _pickAndSendImage,
                    ),
                    IconButton(
                      icon: _isUploadingVoice
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.mic, color: Color(0xFF17C3B2)),
                      onPressed: _isUploadingVoice ? null : _showVoiceRecordingDialog,
                      tooltip: l10n.translate('recordVoice'),
                    ),
                    Expanded(
                      child: TextField(
                        controller: _messageCtrl,
                        minLines: 1,
                        maxLines: 4,
                        textInputAction: TextInputAction.send,
                        onSubmitted: (_) => _sendMessage(),
                        decoration: InputDecoration(
                          hintText: l10n.typeMessage,
                          filled: true,
                          fillColor: Colors.grey.shade100,
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(24),
                            borderSide: BorderSide(color: Colors.grey.shade300),
                          ),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton(
                      icon: const Icon(Icons.send, color: Color(0xFF17C3B2)),
                      onPressed: (_isUploadingImage || _isUploadingVoice) ? null : () => _sendMessage(),
                    ),
                  ],
                ),
              ),
            ),
          ],
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, _) => Center(
          child: Text(
            '${l10n.error}: ${userFriendlyError(l10n, err, logContext: 'Chat load')}',
          ),
        ),
      ),
    );
  }

  String _initials(String name) {
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty || parts.first.isEmpty) return '?';
    if (parts.length == 1) return parts.first.substring(0, 1).toUpperCase();
    return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
  }

  static void _onOpenLink(LinkableElement link) async {
    final uri = Uri.tryParse(link.url);
    if (uri != null && await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  Widget _buildMessageBubble(ChatMessage message) {
    final brand = const Color(0xFF17C3B2);
    
    switch (message.type) {
      case MessageType.text:
        return TextMessageBubble(
          message: message,
          brandColor: brand,
          isMine: message.isMine,
        );
      case MessageType.image:
        return ImageMessageBubble(
          message: message,
          brandColor: brand,
          isMine: message.isMine,
        );
      case MessageType.voice:
        return VoiceMessageBubble(
          message: message,
          brandColor: brand,
          isMine: message.isMine,
        );
      case MessageType.document:
        return DocumentMessageBubble(
          message: message,
          brandColor: brand,
          isMine: message.isMine,
        );
      case MessageType.system:
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: SelectionArea(
            child: Linkify(
              onOpen: _onOpenLink,
              text: message.content.text ?? '',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade600,
                fontStyle: FontStyle.italic,
              ),
              linkStyle: TextStyle(
                fontSize: 12,
                color: Colors.blue.shade700,
                fontStyle: FontStyle.italic,
                decoration: TextDecoration.underline,
              ),
              textAlign: TextAlign.center,
            ),
          ),
        );
    }
  }
}
