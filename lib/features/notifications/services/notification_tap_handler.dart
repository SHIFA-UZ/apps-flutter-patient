import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/models/notification_model.dart';
import 'package:shifa_patient_app_v1/features/notifications/providers/notifications_provider.dart';

/// Central handler for notification tap: mark as read first, then navigate.
/// Used by the single notification callback from [PushNotificationService].
///
/// Standardized payload (backend must send):
///   - type (required): appointment_reminder | new_document | new_message | ...
///   - entityId (required): target entity id (appointment uuid, document id, chat id)
///   - notificationId (required): id to mark as read (PUT /notifications/:id/read)
///   - route (optional): fallback path
///
/// Type → route mapping:
///   appointment_reminder / appointment_* → /bookings/:entityId
///   new_document / document → /documents/:entityId
///   new_message / chat_message → /chat (extra: chatId)
///   task_* → /tasks or /tasks/:taskId/check-in
///   default → /notifications
class NotificationTapHandler {
  NotificationTapHandler._();

  /// Resolve target path from FCM/push payload. Supports:
  /// - Standardized: type, entityId, notificationId, optional route
  /// - Legacy: type (APPOINTMENT_*), appointmentId, documentId, chatId, id
  static String? getTargetPathFromPayload(Map<String, dynamic> data) {
    final typeRaw = (data['type']?.toString() ?? '').toLowerCase().replaceAll('-', '_');
    final type = typeRaw.toUpperCase();
    final entityId = data['entityId']?.toString();
    final appointmentId = _optionalInt(data['appointmentId']) ?? _optionalInt(data['appointment_id']);
    final documentId = data['documentId']?.toString() ?? data['document_id']?.toString();
    final chatId = data['chatId']?.toString() ?? data['chat_id']?.toString();
    final taskId = _optionalInt(data['taskId']) ?? _optionalInt(data['task_id']);

    final id = entityId ?? appointmentId?.toString() ?? documentId ?? chatId;

    switch (typeRaw) {
      case 'signature_requested':
        final pFormId =
            _optionalInt(data['patientFormId']) ?? _optionalInt(data['patient_form_id']);
        if (pFormId != null) return '${AppRoutes.bookings}/sign-form/$pFormId';
        if (id != null && id.isNotEmpty) return '${AppRoutes.bookings}/$id';
        if (appointmentId != null) return '${AppRoutes.bookings}/$appointmentId';
        break;
      case 'appointment_reminder':
      case 'appointment_scheduled':
      case 'appointment_cancelled':
      case 'appointment_changed':
      case 'appointment_confirmed':
      case 'appointment_upcoming':
      case 'ai_visit_summary_ready':
        if (typeRaw == 'ai_visit_summary_ready' && id != null && id.isNotEmpty) {
          return '${AppRoutes.bookings}/$id/visit-summary';
        }
        if (id != null && id.isNotEmpty) return '${AppRoutes.bookings}/$id';
        if (appointmentId != null) return '${AppRoutes.bookings}/$appointmentId';
        break;
      case 'consultation_payment_reminder':
      case 'consultation_payment_due_24h':
      case 'consultation_payment_due_6h':
      case 'consultation_payment_due_1h':
        if (id != null && id.isNotEmpty) return '${AppRoutes.bookings}/$id/pay';
        if (appointmentId != null) return '${AppRoutes.bookings}/$appointmentId/pay';
        break;
      case 'treatment_plan_payment_reminder':
      case 'treatment_plan_updated':
        final planId = _optionalInt(data['treatmentPlanId']) ?? _optionalInt(data['treatment_plan_id']);
        if (planId != null) return '${AppRoutes.bookings}/treatment-plan/$planId';
        break;
      case 'prophylaxis_reminder':
        return AppRoutes.bookings;
      case 'new_document':
      case 'document':
        if (id != null && id.isNotEmpty) return '${AppRoutes.documents}/$id';
        if (documentId != null && documentId.isNotEmpty) return '${AppRoutes.documents}/$documentId';
        break;
      case 'new_message':
      case 'chat_message':
        return AppRoutes.chat;
      case 'task_assigned':
      case 'task_reminder':
      case 'task_cancelled':
      case 'task_completed':
        if (taskId != null) return '${AppRoutes.tasks}/$taskId/check-in';
        return AppRoutes.tasks;
    }

    if (type == 'SIGNATURE_REQUESTED') {
      final pFormId =
          _optionalInt(data['patientFormId']) ?? _optionalInt(data['patient_form_id']);
      if (pFormId != null) return '${AppRoutes.bookings}/sign-form/$pFormId';
    }
    if ((type == 'AI_VISIT_SUMMARY_READY') && (appointmentId != null || (id != null && id.isNotEmpty))) {
      return '${AppRoutes.bookings}/${appointmentId ?? id}/visit-summary';
    }
    if (_isConsultationPaymentPaywallType(type) &&
        (appointmentId != null || (id != null && id.isNotEmpty))) {
      return '${AppRoutes.bookings}/${appointmentId ?? id}/pay';
    }
    if (_isTreatmentPlanType(type)) {
      final planId = _optionalInt(data['treatmentPlanId']) ?? _optionalInt(data['treatment_plan_id']);
      if (planId != null) return '${AppRoutes.bookings}/treatment-plan/$planId';
    }
    if (type == 'PROPHYLAXIS_REMINDER') {
      return AppRoutes.bookings;
    }
    if (_isAppointmentType(type) && (appointmentId != null || (id != null && id.isNotEmpty))) {
      return '${AppRoutes.bookings}/${appointmentId ?? id}';
    }
    if (documentId != null && documentId.isNotEmpty && !type.contains('ACCESS')) {
      return '${AppRoutes.documents}/$documentId';
    }
    if ((type == 'NEW_MESSAGE' || type == 'CHAT_MESSAGE') && (chatId != null || entityId != null)) {
      return AppRoutes.chat;
    }
    if (_isTaskType(type)) {
      if (taskId != null) return '${AppRoutes.tasks}/$taskId/check-in';
      return AppRoutes.tasks;
    }

    final route = data['route']?.toString();
    if (route != null && route.isNotEmpty) {
      if (route.startsWith('/')) return route;
      return '/$route';
    }
    return null;
  }

  static int? _optionalInt(dynamic v) {
    if (v == null) return null;
    if (v is int) return v;
    if (v is num) return v.toInt();
    if (v is String) return int.tryParse(v);
    return null;
  }

  static bool _isConsultationPaymentPaywallType(String type) {
    return type == 'CONSULTATION_PAYMENT_REMINDER' ||
        type == 'CONSULTATION_PAYMENT_DUE_24H' ||
        type == 'CONSULTATION_PAYMENT_DUE_6H' ||
        type == 'CONSULTATION_PAYMENT_DUE_1H';
  }

  static bool _isTreatmentPlanType(String type) {
    return type.contains('TREATMENT_PLAN');
  }

  static bool _isAppointmentType(String type) {
    return type.contains('APPOINTMENT') || type == 'SIGNATURE_REQUESTED';
  }

  static bool _isTaskType(String type) {
    return type.contains('TASK');
  }

  /// When user taps any received (push) notification: mark as read, then open Notification Center.
  /// Updates local notification list and unread badge via controller. Does not block on API failure.
  static Future<void> handleTapFromPayload({
    required BuildContext context,
    required Map<String, dynamic> data,
    required NotificationsController controller,
    required String Function(String key) translate,
  }) async {
    final notificationIdRaw = data['notificationId'] ?? data['id'];
    if (notificationIdRaw != null) {
      try {
        if (notificationIdRaw is int && notificationIdRaw > 0) {
          await controller.markAsRead(notificationIdRaw);
        } else if (notificationIdRaw is String) {
          final id = int.tryParse(notificationIdRaw);
          if (id != null && id > 0) {
            await controller.markAsRead(id);
          } else {
            await controller.markAsReadById(notificationIdRaw);
          }
        } else {
          await controller.markAsReadById(notificationIdRaw.toString());
        }
      } catch (e) {
        AppLogger.error('Mark notification as read failed:', e);
      }
    }

    if (!context.mounted) return;
    final path = getTargetPathFromPayload(data);
    if (path != null && path.isNotEmpty) {
      if (path == AppRoutes.chat) {
        final chatId = data['chatId']?.toString() ?? data['chat_id']?.toString();
        if (chatId != null && chatId.isNotEmpty) {
          context.go(path, extra: {'chatId': chatId});
        } else {
          context.go(path);
        }
      } else {
        context.go(path);
      }
      return;
    }
    context.go(AppRoutes.notifications);
  }

  /// Returns the route path for a [NotificationModel] (used by in-app dialog and legacy flow).
  static String? getTargetLocation(NotificationModel notification) {
    final type = notification.type.toUpperCase();
    final appointmentId = notification.appointmentId;
    final documentId = notification.documentId;
    final chatId = notification.chatId;
    final taskId = notification.taskId;

    if (notification.isDocumentAccessRequest) return null;
    if (_isTreatmentPlanType(type) && notification.treatmentPlanId != null) {
      return '${AppRoutes.bookings}/treatment-plan/${notification.treatmentPlanId}';
    }
    if (type == 'PROPHYLAXIS_REMINDER') {
      return AppRoutes.bookings;
    }
    if (type == 'SIGNATURE_REQUESTED' && notification.patientFormId != null) {
      return '${AppRoutes.bookings}/sign-form/${notification.patientFormId}';
    }
    if (_isTaskType(type)) {
      if (taskId != null) return '${AppRoutes.tasks}/$taskId/check-in';
      return AppRoutes.tasks;
    }
    if ((type == 'AI_SCRIBE_READY' || type == 'AI_VISIT_SUMMARY_READY') && appointmentId != null) {
      return '${AppRoutes.bookings}/$appointmentId/visit-summary';
    }
    if (_isConsultationPaymentPaywallType(type) && appointmentId != null) {
      return '${AppRoutes.bookings}/$appointmentId/pay';
    }
    if (type == 'SIGNATURE_REQUESTED') {
      return appointmentId != null ? '${AppRoutes.bookings}/$appointmentId' : null;
    }
    if (notification.title.toLowerCase().contains('signature') && appointmentId != null) {
      return '${AppRoutes.bookings}/$appointmentId';
    }
    if (_isAppointmentType(type) && appointmentId != null) {
      return '${AppRoutes.bookings}/$appointmentId';
    }
    if (appointmentId != null && documentId == null) return '${AppRoutes.bookings}/$appointmentId';
    if (documentId != null && documentId.isNotEmpty) return '${AppRoutes.documents}/$documentId';
    if ((type == 'NEW_MESSAGE' || type == 'CHAT_MESSAGE') && chatId != null && chatId.isNotEmpty) {
      return AppRoutes.chat;
    }
    return null;
  }

  /// Legacy: handle tap from NotificationModel (in-app list/dialog). Mark as read then navigate.
  static Future<bool> handleTap({
    required BuildContext context,
    required NotificationModel notification,
    required Future<void> Function(int notificationId) markAsRead,
    required String Function(String key) translate,
  }) async {
    if (notification.isDocumentAccessRequest) return false;

    if (!notification.isRead && notification.id > 0) {
      try {
        await markAsRead(notification.id);
      } catch (e) {
        AppLogger.error('Mark as read failed:', e);
      }
    }

    final path = getTargetLocation(notification);
    if (path == null) {
      if (_isAppointmentType(notification.type) || notification.title.toLowerCase().contains('signature')) {
        _showFallback(context, translate);
      }
      return false;
    }
    if (context.mounted) {
      if (path == AppRoutes.chat && notification.chatId != null) {
        context.go(path, extra: {'chatId': notification.chatId});
      } else {
        context.go(path);
      }
    }
    return true;
  }

  static void _showFallback(BuildContext context, String Function(String key) translate) {
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(translate('notificationCannotOpen'))));
  }
}
