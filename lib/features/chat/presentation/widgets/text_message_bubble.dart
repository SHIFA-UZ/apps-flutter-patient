// lib/features/chat/presentation/widgets/text_message_bubble.dart
import 'package:flutter/material.dart';
import 'package:flutter_linkify/flutter_linkify.dart';
import 'package:shifa_patient_app_v1/features/chat/domain/chat_models.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:url_launcher/url_launcher.dart';

class TextMessageBubble extends StatelessWidget {
  final ChatMessage message;
  final Color brandColor;
  final bool isMine;

  const TextMessageBubble({
    Key? key,
    required this.message,
    required this.brandColor,
    required this.isMine,
  }) : super(key: key);

  String _formatTime(DateTime dateTime) {
    return '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
  }

  static void _onOpenLink(LinkableElement link) async {
    final uri = Uri.tryParse(link.url);
    if (uri != null && await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  Widget _buildStatusIcon(MessageStatus status) {
    switch (status) {
      case MessageStatus.sending:
        return SizedBox(
          width: 12,
          height: 12,
          child: CircularProgressIndicator(
            strokeWidth: 1.5,
            valueColor: AlwaysStoppedAnimation<Color>(Colors.white70),
          ),
        );
      case MessageStatus.sent:
        return Icon(Icons.check, size: 14, color: Colors.white70);
      case MessageStatus.delivered:
        return Icon(Icons.done_all, size: 14, color: Colors.white70);
      case MessageStatus.read:
        return Icon(Icons.done_all, size: 14, color: Colors.blue.shade300);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bubbleColor = isMine ? brandColor : Colors.grey.shade200;
    final textColor = isMine ? Colors.white : Colors.black87;
    final senderLabel = message.senderRole == SenderRole.doctor
        ? AppLocalizations.of(context)!.doctor
        : AppLocalizations.of(context)!.patient;

    return Container(
      constraints: const BoxConstraints(maxWidth: 520),
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Column(
        crossAxisAlignment: isMine ? CrossAxisAlignment.end : CrossAxisAlignment.start,
        children: [
          // Sender label (only show if not mine)
          if (!isMine)
            Padding(
              padding: const EdgeInsets.only(left: 4, bottom: 2),
              child: Text(
                senderLabel,
                style: TextStyle(
                  fontSize: 11,
                  color: Colors.grey.shade600,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          // Message bubble: text is copiable (SelectionArea) and links are clickable (Linkify)
          Container(
            decoration: BoxDecoration(
              color: bubbleColor,
              borderRadius: BorderRadius.only(
                topLeft: const Radius.circular(16),
                topRight: const Radius.circular(16),
                bottomLeft: Radius.circular(isMine ? 16 : 4),
                bottomRight: Radius.circular(isMine ? 4 : 16),
              ),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: SelectionArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (message.content.text != null && message.content.text!.isNotEmpty)
                    Linkify(
                      onOpen: _onOpenLink,
                      text: message.content.text!,
                      style: TextStyle(
                        fontSize: 15,
                        color: textColor,
                        height: 1.4,
                      ),
                      linkStyle: TextStyle(
                        fontSize: 15,
                        color: textColor,
                        height: 1.4,
                        decoration: TextDecoration.underline,
                        decorationColor: isMine ? Colors.white70 : Colors.blue.shade700,
                      ),
                    ),
                  const SizedBox(height: 4),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      SelectableText(
                        _formatTime(message.sentAt),
                        style: TextStyle(
                          fontSize: 11,
                          color: isMine ? Colors.white70 : Colors.grey.shade600,
                        ),
                      ),
                      if (isMine) ...[
                        const SizedBox(width: 4),
                        _buildStatusIcon(message.status),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
