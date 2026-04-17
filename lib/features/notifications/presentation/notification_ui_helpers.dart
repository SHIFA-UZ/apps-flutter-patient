import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';

/// Relative time for notifications: "5 min ago", "Yesterday 23:11", "Mar 5 • 23:11".
String formatNotificationTime(DateTime dateTime, AppLocalizations l10n) {
  final now = DateTime.now();
  final local = dateTime.toLocal();
  final today = DateTime(now.year, now.month, now.day);
  final yesterday = today.subtract(const Duration(days: 1));
  final dtDate = DateTime(local.year, local.month, local.day);

  final diff = now.difference(local);
  if (diff.inMinutes < 1) return l10n.translate('justNow');
  if (diff.inMinutes < 60) {
    final n = diff.inMinutes;
    return n == 1
        ? l10n.translate('minuteAgo')
        : l10n.translate('minutesAgo').replaceAll('%s', '$n');
  }
  final h = local.hour.toString().padLeft(2, '0');
  final m = local.minute.toString().padLeft(2, '0');
  final timeStr = '$h:$m';
  if (diff.inHours < 24 && dtDate == today) return timeStr;
  if (dtDate == yesterday) {
    final template = l10n.translate('timeYesterday');
    if (template.contains('{time}')) {
      return template.replaceAll('{time}', timeStr);
    }
    return '${l10n.translate('yesterday')} $timeStr';
  }
  final monthStr = DateFormat.MMM(l10n.locale.toString()).format(local);
  return '$monthStr ${local.day} • $timeStr';
}

/// Section header: "Today", "Yesterday", or "Mar 5".
String dateSectionLabel(DateTime dateTime, AppLocalizations l10n) {
  final now = DateTime.now();
  final local = dateTime.toLocal();
  final today = DateTime(now.year, now.month, now.day);
  final yesterday = today.subtract(const Duration(days: 1));
  final dtDate = DateTime(local.year, local.month, local.day);

  if (dtDate == today) return l10n.translate('today');
  if (dtDate == yesterday) return l10n.translate('yesterday');
  final monthStr = DateFormat.MMM(l10n.locale.toString()).format(local);
  return '$monthStr ${local.day}';
}

/// Semantic color and icon for notification type.
({Color color, IconData icon}) styleForNotificationType(String type) {
  switch (type.toUpperCase()) {
    case 'APPOINTMENT_SCHEDULED':
    case 'NEW_APPOINTMENT':
    case 'APPOINTMENT_REMINDER':
    case 'APPOINTMENT_UPCOMING':
    case 'APPOINTMENT_SOON':
    case 'APPOINTMENT_CLOSE':
    case 'APPOINTMENT_CHANGED':
      return (color: const Color(0xFF1976D2), icon: Icons.calendar_today_rounded); // Blue
    case 'APPOINTMENT_CANCELLED':
      return (color: const Color(0xFFC62828), icon: Icons.event_busy_rounded); // Red
    case 'SIGNATURE_REQUESTED':
      return (color: const Color(0xFF6A1B9A), icon: Icons.draw_rounded); // Purple
    case 'DOCUMENT_ACCESS_REQUEST':
      return (color: const Color(0xFF00897B), icon: Icons.lock_open_rounded); // Teal
    case 'TASK_ASSIGNED':
    case 'TASK_REMINDER':
      return (color: const Color(0xFFF9A825), icon: Icons.assignment_rounded); // Amber
    case 'TASK_CANCELLED':
      return (color: const Color(0xFF616161), icon: Icons.cancel_rounded); // Grey
    default:
      return (color: const Color(0xFF616161), icon: Icons.notifications_rounded); // Grey
  }
}

/// Filter categories for notifications.
enum NotificationFilter {
  all,
  appointments,
  documents,
  tasks,
}

bool notificationMatchesFilter(String type, NotificationFilter filter) {
  switch (filter) {
    case NotificationFilter.all:
      return true;
    case NotificationFilter.appointments:
      return type.toUpperCase().contains('APPOINTMENT');
    case NotificationFilter.documents:
      return type.toUpperCase().contains('DOCUMENT') ||
          type.toUpperCase().contains('SIGNATURE');
    case NotificationFilter.tasks:
      return type.toUpperCase().contains('TASK');
  }
}
