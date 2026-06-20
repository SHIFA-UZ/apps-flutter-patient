import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/notification_model.dart';

/// Shared logic to get localized notification title and message for in-app dialog,
/// notification list, and system tray. Use so backend-sent text is replaced by
/// locale-specific strings for known types.
class NotificationLocalization {
  NotificationLocalization._();

  static String getTitle(NotificationModel notification, AppLocalizations l10n) {
    return getTitleForType(notification.type, l10n, fallbackTitle: notification.title);
  }

  static String getMessage(NotificationModel notification, AppLocalizations l10n) {
    final type = notification.type.toUpperCase();
    final doctorName = notification.doctorName ?? l10n.doctor;
    switch (type) {
      case 'APPOINTMENT_CANCELLED':
      case 'APPOINTMENT_CANCELLED_BY_PATIENT':
        return l10n.translate('notificationMessageCancelled');
      case 'APPOINTMENT_CHANGED':
      case 'APPOINTMENT_RESCHEDULED_BY_PATIENT':
        return l10n.translate('notificationMessageChanged');
      case 'APPOINTMENT_REMINDER':
      case 'APPOINTMENT_UPCOMING':
      case 'APPOINTMENT_SOON':
      case 'APPOINTMENT_CLOSE':
        return l10n.translate('notificationMessageReminder');
      case 'APPOINTMENT_SCHEDULED':
      case 'NEW_APPOINTMENT':
      case 'APPOINTMENT_ASSIGNED':
      case 'APPOINTMENT_BOOKED_BY_PATIENT':
        return l10n.translate('notificationMessageScheduled');
      case 'SIGNATURE_REQUESTED':
        return l10n.translate('signatureRequestedMessage').replaceAll('{doctorName}', doctorName);
      case 'CONSULTATION_PAYMENT_REMINDER':
        return l10n.translate('consultationPaymentReminderMessage');
      case 'CONSULTATION_PAYMENT_DUE_24H':
        return l10n.translate('consultationPaymentDue24hMessage');
      case 'CONSULTATION_PAYMENT_DUE_6H':
        return l10n.translate('consultationPaymentDue6hMessage');
      case 'CONSULTATION_PAYMENT_DUE_1H':
        return l10n.translate('consultationPaymentDue1hMessage');
      case 'DOCUMENT_ACCESS_REQUEST':
        return _localizedDocumentAccessMessage(notification, l10n);
      case 'DOCUMENT_ACCESS_APPROVED':
        return l10n.translate('documentAccessApprovedMessage');
      case 'DOCUMENT_ACCESS_REJECTED':
        return l10n.translate('documentAccessRejectedMessage');
      case 'INSTALLMENT_DUE_SOON':
        return l10n.notificationInstallmentDueSoonMessage;
      case 'INSTALLMENT_DUE_TODAY':
        return l10n.notificationInstallmentDueTodayMessage;
      case 'INSTALLMENT_OVERDUE':
        return l10n.notificationInstallmentOverdueMessage;
      case 'INSTALLMENT_SCHEDULE_CREATED':
        return l10n.notificationInstallmentScheduleCreatedMessage;
      case 'AI_VISIT_SUMMARY_READY':
      case 'AI_SCRIBE_READY':
        return l10n.translate('visitSummaryReadyMessage');
      case 'CHAT_MESSAGE':
      case 'NEW_MESSAGE':
        return l10n.translate('chatNewMessageMessage');
      case 'TASK_REMINDER':
        return l10n.translate('taskReminderMessage');
      case 'TASK_ASSIGNED':
        return l10n.translate('taskAssignedMessage');
      case 'TASK_CANCELLED':
        return l10n.translate('taskCancelledMessage');
      case 'TASK_COMPLETED':
        return l10n.translate('taskCompletedMessage');
      case 'PROPHYLAXIS_REMINDER':
        return l10n.translate('prophylaxisReminderMessage');
      case 'TREATMENT_PLAN_PAYMENT_REMINDER':
        return l10n.translate('treatmentPlanPaymentReminderMessage');
      case 'TREATMENT_PLAN_UPDATED':
        return l10n.translate('treatmentPlanUpdatedMessage');
      case 'NEW_DOCUMENT':
      case 'DOCUMENT':
        return l10n.translate('newDocumentMessage');
      default:
        if (notification.title.toLowerCase().contains('signature')) {
          return l10n.translate('signatureRequestedMessage').replaceAll('{doctorName}', doctorName);
        }
        return notification.message;
    }
  }

  /// Localized title/body for FCM tray when payload includes a known [type].
  static ({String title, String body}) localizedPushText({
    required Map<String, dynamic> data,
    required AppLocalizations l10n,
    String? fallbackTitle,
    String? fallbackBody,
  }) {
    final type = (data['type']?.toString() ?? '').toUpperCase().replaceAll('-', '_');
    if (type.isEmpty) {
      return (
        title: fallbackTitle?.trim().isNotEmpty == true
            ? fallbackTitle!.trim()
            : l10n.translate('pushNewNotification'),
        body: fallbackBody?.trim() ?? '',
      );
    }
    final model = NotificationModel.fromFcmData(data);
    if (model == null) {
      return (
        title: fallbackTitle?.trim().isNotEmpty == true
            ? fallbackTitle!.trim()
            : l10n.translate('pushNewNotification'),
        body: fallbackBody?.trim() ?? '',
      );
    }
    final title = getTitle(model, l10n);
    final body = getMessage(model, l10n);
    return (
      title: title.isNotEmpty ? title : (fallbackTitle ?? l10n.translate('pushNewNotification')),
      body: body.isNotEmpty ? body : (fallbackBody ?? ''),
    );
  }

  static String getTitleForType(
    String typeRaw,
    AppLocalizations l10n, {
    String? fallbackTitle,
  }) {
    final type = typeRaw.toUpperCase().replaceAll('-', '_');
    switch (type) {
      case 'CHAT_MESSAGE':
      case 'NEW_MESSAGE':
        return l10n.translate('chatNewMessageTitle');
      case 'AI_VISIT_SUMMARY_READY':
      case 'AI_SCRIBE_READY':
        return l10n.translate('visitSummaryReadyTitle');
      case 'TASK_REMINDER':
        return l10n.translate('taskReminderTitle');
      case 'TASK_ASSIGNED':
        return l10n.translate('taskAssignedTitle');
      case 'TASK_CANCELLED':
        return l10n.translate('taskCancelledTitle');
      case 'TASK_COMPLETED':
        return l10n.translate('taskCompletedTitle');
      case 'APPOINTMENT_CANCELLED':
      case 'APPOINTMENT_CANCELLED_BY_PATIENT':
        return l10n.translate('appointmentCancelledTitle');
      case 'APPOINTMENT_CHANGED':
      case 'APPOINTMENT_RESCHEDULED_BY_PATIENT':
        return l10n.translate('appointmentChangedTitle');
      case 'APPOINTMENT_REMINDER':
      case 'APPOINTMENT_UPCOMING':
      case 'APPOINTMENT_SOON':
      case 'APPOINTMENT_CLOSE':
        return l10n.translate('appointmentReminderTitle');
      case 'APPOINTMENT_SCHEDULED':
      case 'NEW_APPOINTMENT':
      case 'APPOINTMENT_ASSIGNED':
      case 'APPOINTMENT_BOOKED_BY_PATIENT':
        return l10n.translate('newAppointmentScheduledTitle');
      case 'SIGNATURE_REQUESTED':
        return l10n.translate('signatureRequestedTitle');
      case 'CONSULTATION_PAYMENT_REMINDER':
      case 'CONSULTATION_PAYMENT_DUE_24H':
      case 'CONSULTATION_PAYMENT_DUE_6H':
      case 'CONSULTATION_PAYMENT_DUE_1H':
        return l10n.translate('consultationPaymentReminderTitle');
      case 'DOCUMENT_ACCESS_REQUEST':
        return l10n.translate('documentAccessRequestTitle');
      case 'DOCUMENT_ACCESS_APPROVED':
        return l10n.translate('documentAccessApprovedTitle');
      case 'DOCUMENT_ACCESS_REJECTED':
        return l10n.translate('documentAccessRejectedTitle');
      case 'INSTALLMENT_DUE_SOON':
        return l10n.notificationInstallmentDueSoonTitle;
      case 'INSTALLMENT_DUE_TODAY':
        return l10n.notificationInstallmentDueTodayTitle;
      case 'INSTALLMENT_OVERDUE':
        return l10n.notificationInstallmentOverdueTitle;
      case 'INSTALLMENT_SCHEDULE_CREATED':
        return l10n.notificationInstallmentScheduleCreatedTitle;
      case 'PROPHYLAXIS_REMINDER':
        return l10n.translate('prophylaxisReminderTitle');
      case 'TREATMENT_PLAN_PAYMENT_REMINDER':
        return l10n.translate('treatmentPlanPaymentReminderTitle');
      case 'TREATMENT_PLAN_UPDATED':
        return l10n.translate('treatmentPlanUpdatedTitle');
      case 'NEW_DOCUMENT':
      case 'DOCUMENT':
        return l10n.translate('newDocumentTitle');
      default:
        if (fallbackTitle != null && fallbackTitle.toLowerCase().contains('signature')) {
          return l10n.translate('signatureRequestedTitle');
        }
        return fallbackTitle?.trim().isNotEmpty == true
            ? fallbackTitle!.trim()
            : l10n.translate('defaultNotificationTitle');
    }
  }

  /// Parses backend message "X requested access to "Y" for Z." and returns (requester, fileName, patient) or null.
  static ({String requesterName, String fileName, String patientName})? parseDocumentAccessMessage(String message) {
    const prefix = ' requested access to ';
    const infix = ' for ';
    final i = message.indexOf(prefix);
    if (i < 0) return null;
    final requesterName = message.substring(0, i).trim();
    if (requesterName.isEmpty) return null;
    final rest = message.substring(i + prefix.length);
    final j = rest.indexOf(infix);
    if (j < 0) return null;
    String fileName = rest.substring(0, j).trim();
    if (fileName.startsWith('"') || fileName.startsWith("'")) fileName = fileName.substring(1);
    if (fileName.endsWith('"') || fileName.endsWith("'")) fileName = fileName.substring(0, fileName.length - 1);
    fileName = fileName.trim();
    final patientName = rest.substring(j + infix.length).trim().replaceFirst(RegExp(r'\.$'), '').trim();
    if (fileName.isEmpty || patientName.isEmpty) return null;
    return (requesterName: requesterName, fileName: fileName, patientName: patientName);
  }

  static String _localizedDocumentAccessMessage(NotificationModel notification, AppLocalizations l10n) {
    final template = l10n.translate('documentAccessRequestMessage');
    if (template.isEmpty || template == 'documentAccessRequestMessage') return notification.message;
    final parsed = parseDocumentAccessMessage(notification.message);
    if (parsed == null) return notification.message;
    return template
        .replaceAll('{requesterName}', parsed.requesterName)
        .replaceAll('{fileName}', parsed.fileName)
        .replaceAll('{patientName}', parsed.patientName);
  }
}
