import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/notification_model.dart';
import 'package:shifa_patient_app_v1/features/notifications/presentation/notification_ui_helpers.dart';
import 'package:shifa_patient_app_v1/features/notifications/services/notification_tap_handler.dart';
import 'package:shifa_patient_app_v1/features/notifications/utils/notification_localization.dart';

/// Bottom sheet that surfaces a notification's full info even when no deep
/// link is available. Always provides the localized title, full message,
/// timestamp, and — when applicable — a single primary action that takes the
/// patient to the relevant screen (treatment plan summary, appointment,
/// document, etc.).
///
/// Existing UX rule: tapping a card on the notifications list opens this
/// sheet so the patient always sees readable info. Deep links are still
/// honoured via the primary action button rather than silently navigating
/// (avoids the "tapping does nothing" failure mode reported by users when
/// the underlying target couldn't be resolved).
class NotificationDetailsSheet extends StatelessWidget {
  final NotificationModel notification;

  const NotificationDetailsSheet({super.key, required this.notification});

  static Future<void> show({
    required BuildContext context,
    required NotificationModel notification,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      useSafeArea: true,
      builder: (ctx) => NotificationDetailsSheet(notification: notification),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final style = styleForNotificationType(notification.type);
    final title = NotificationLocalization.getTitle(notification, l10n);
    final message = NotificationLocalization.getMessage(notification, l10n);
    final timeStr = formatNotificationTime(notification.createdAt, l10n);
    final dateStr = _absoluteDate(notification.createdAt);
    final actionRoute = NotificationTapHandler.getTargetLocation(notification);
    final actionLabel = _actionLabelFor(notification.type, l10n);
    final canOpen = actionRoute != null && actionLabel != null;

    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  decoration: BoxDecoration(
                    color: style.color.withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  padding: const EdgeInsets.all(10),
                  child: Icon(style.icon, color: style.color, size: 22),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w700,
                              color: style.color,
                            ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '$timeStr  •  $dateStr',
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: Colors.grey.shade600,
                            ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 280),
              child: SingleChildScrollView(
                child: Text(
                  message,
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        height: 1.45,
                      ),
                ),
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: Text(l10n.translate('close')),
                  ),
                ),
                if (canOpen) ...[
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: () {
                        Navigator.of(context).pop();
                        _openRoute(context, notification, actionRoute);
                      },
                      icon: const Icon(Icons.arrow_forward_rounded, size: 18),
                      label: Text(actionLabel),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _openRoute(
    BuildContext context,
    NotificationModel n,
    String route,
  ) {
    if (route == AppRoutes.chat && (n.chatId ?? '').isNotEmpty) {
      context.go(route, extra: {'chatId': n.chatId});
    } else {
      context.go(route);
    }
  }

  static String _absoluteDate(DateTime dt) {
    final local = dt.toLocal();
    String two(int v) => v.toString().padLeft(2, '0');
    return '${local.year}-${two(local.month)}-${two(local.day)} '
        '${two(local.hour)}:${two(local.minute)}';
  }

  static String? _actionLabelFor(String type, AppLocalizations l10n) {
    final u = type.toUpperCase();
    if (u.contains('TREATMENT_PLAN') ||
        u == 'INSTALLMENT_DUE_SOON' ||
        u == 'INSTALLMENT_DUE_TODAY' ||
        u == 'INSTALLMENT_OVERDUE' ||
        u == 'INSTALLMENT_SCHEDULE_CREATED') {
      return l10n.translate('notificationActionOpenTreatmentPlan');
    }
    if (u == 'CONSULTATION_PAYMENT_REMINDER' ||
        u == 'CONSULTATION_PAYMENT_DUE_24H' ||
        u == 'CONSULTATION_PAYMENT_DUE_6H' ||
        u == 'CONSULTATION_PAYMENT_DUE_1H') {
      return l10n.translate('notificationActionOpenPayment');
    }
    if (u == 'SIGNATURE_REQUESTED') {
      return l10n.translate('notificationActionOpenForm');
    }
    if (u == 'PROPHYLAXIS_REMINDER') {
      return l10n.translate('notificationActionOpenBookings');
    }
    if (u.contains('APPOINTMENT')) {
      return l10n.translate('notificationActionOpenAppointment');
    }
    if (u == 'NEW_DOCUMENT' || u == 'DOCUMENT') {
      return l10n.translate('notificationActionViewDocument');
    }
    if (u == 'NEW_MESSAGE' || u == 'CHAT_MESSAGE') {
      return l10n.translate('notificationActionOpenChat');
    }
    if (u.contains('TASK')) {
      return l10n.translate('notificationActionOpenTask');
    }
    if (u == 'AI_VISIT_SUMMARY_READY' || u == 'AI_SCRIBE_READY') {
      return l10n.translate('notificationActionViewSummary');
    }
    return l10n.translate('notificationActionOpenDetails');
  }
}
