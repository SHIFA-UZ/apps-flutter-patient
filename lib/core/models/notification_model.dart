import 'package:equatable/equatable.dart';

class NotificationModel extends Equatable {
  final int id;
  final String title;
  final String message;
  final String type;
  final int? appointmentId;
  /// Present when type is SIGNATURE_REQUESTED for medical **form** 025-2 (not appointment signing).
  final int? patientFormId;
  /// Present when type is DOCUMENT_ACCESS_REQUEST; use for approve/reject and deep linking.
  final int? documentAccessRequestId;
  /// Server-side status for DOCUMENT_ACCESS_REQUEST: "pending" | "approved" | "rejected".
  /// Used to hide approve/reject buttons once the request is resolved.
  final String? documentAccessRequestStatus;
  /// Present for TASK_ASSIGNED, TASK_CANCELLED, TASK_REMINDER; use for navigation to tasks.
  final int? taskId;
  /// Treatment plan notifications (`TREATMENT_PLAN_*`); deep link to summary screen.
  final int? treatmentPlanId;
  /// Optional payload from backend (e.g. data.documentId, data.chatId, data.doctorName).
  final String? documentId;
  final String? chatId;
  final String? doctorName;
  final DateTime createdAt;
  final DateTime? readAt;

  const NotificationModel({
    required this.id,
    required this.title,
    required this.message,
    required this.type,
    this.appointmentId,
    this.patientFormId,
    this.documentAccessRequestId,
    this.documentAccessRequestStatus,
    this.taskId,
    this.treatmentPlanId,
    this.documentId,
    this.chatId,
    this.doctorName,
    required this.createdAt,
    this.readAt,
  });

  bool get isRead => readAt != null;

  bool get isDocumentAccessRequest =>
      type == 'DOCUMENT_ACCESS_REQUEST' && documentAccessRequestId != null;

  /// True when this is a document-access notification AND the underlying
  /// request is still awaiting the patient's decision. False once the
  /// request has been approved or rejected (so the UI can hide the
  /// approve/reject buttons).
  bool get isDocumentAccessRequestPending =>
      isDocumentAccessRequest &&
      (documentAccessRequestStatus == null ||
          documentAccessRequestStatus!.toLowerCase() == 'pending');

  factory NotificationModel.fromJson(Map<String, dynamic> json) {
    final data = json['data'] is Map<String, dynamic> ? json['data'] as Map<String, dynamic> : null;
    return NotificationModel(
      id: (json['id'] is num) ? (json['id'] as num).toInt() : json['id'] as int,
      title: json['title'] as String,
      message: json['message'] as String,
      type: json['type'] as String,
      appointmentId: _optionalInt(json['appointmentId']) ??
          _optionalInt(json['appointment_id']) ??
          (data != null ? _optionalInt(data['appointmentId']) ?? _optionalInt(data['appointment_id']) : null),
      patientFormId: _optionalInt(json['patientFormId']) ??
          _optionalInt(json['patient_form_id']) ??
          (data != null
              ? _optionalInt(data['patientFormId']) ?? _optionalInt(data['patient_form_id'])
              : null),
      documentAccessRequestId: _optionalInt(json['documentAccessRequestId']) ?? _optionalInt(json['document_access_request_id']),
      documentAccessRequestStatus: (json['documentAccessRequestStatus'] as String?)
          ?? (json['document_access_request_status'] as String?),
      taskId: _optionalInt(json['taskId']) ?? _optionalInt(json['task_id']) ??
          (data != null ? _optionalInt(data['taskId']) ?? _optionalInt(data['task_id']) : null),
      treatmentPlanId: _optionalInt(json['treatmentPlanId']) ??
          _optionalInt(json['treatment_plan_id']) ??
          (data != null
              ? _optionalInt(data['treatmentPlanId']) ?? _optionalInt(data['treatment_plan_id'])
              : null),
      documentId: json['documentId']?.toString() ??
          json['document_id']?.toString() ??
          data?['documentId']?.toString() ??
          data?['document_id']?.toString(),
      chatId: json['chatId']?.toString() ?? json['chat_id']?.toString() ?? data?['chatId']?.toString() ?? data?['chat_id']?.toString(),
      doctorName: json['doctorName'] as String? ?? json['doctor_name'] as String? ?? data?['doctorName'] as String? ?? data?['doctor_name'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      readAt: json['readAt'] != null ? DateTime.parse(json['readAt'] as String) : null,
    );
  }

  static int? _optionalInt(dynamic v) {
    if (v == null) return null;
    if (v is int) return v;
    if (v is num) return v.toInt();
    if (v is String) return int.tryParse(v);
    return null;
  }

  /// Build from FCM data payload (all values may be strings).
  /// Supports standardized payload (type, entityId, notificationId) and legacy (appointmentId, documentId, id).
  static NotificationModel? fromFcmData(Map<String, dynamic> data) {
    if (data.isEmpty) return null;
    final typeRaw = data['type']?.toString() ?? 'GENERAL';
    final type = typeRaw.toUpperCase().replaceAll('-', '_');
    final entityId = data['entityId']?.toString();
    final id = _optionalInt(data['notificationId']) ?? _optionalInt(data['id']) ?? 0;
    final createdAtStr = data['createdAt']?.toString() ?? data['created_at']?.toString();
    final createdAt = createdAtStr != null ? DateTime.tryParse(createdAtStr) ?? DateTime.now() : DateTime.now();

    int? appointmentId = _optionalInt(data['appointmentId']) ?? _optionalInt(data['appointment_id']);
    int? patientFormId = _optionalInt(data['patientFormId']) ?? _optionalInt(data['patient_form_id']);
    String? documentId = data['documentId']?.toString() ?? data['document_id']?.toString();
    String? chatId = data['chatId']?.toString() ?? data['chat_id']?.toString();
    int? taskId = _optionalInt(data['taskId']) ?? _optionalInt(data['task_id']);
    int? treatmentPlanId =
        _optionalInt(data['treatmentPlanId']) ?? _optionalInt(data['treatment_plan_id']);

    if (entityId != null && entityId.isNotEmpty) {
      if (_isAppointmentTypeKey(type) || typeRaw.toLowerCase().contains('appointment')) {
        appointmentId = int.tryParse(entityId) ?? appointmentId;
      } else if (_isTreatmentPlanTypeKey(type)) {
        treatmentPlanId = int.tryParse(entityId) ?? treatmentPlanId;
      } else if (_isDocumentTypeKey(type) || typeRaw.toLowerCase().contains('document')) {
        documentId = entityId;
      } else if (_isChatTypeKey(type) || typeRaw.toLowerCase().contains('message')) {
        chatId = entityId;
      } else if (_isTaskTypeKey(type)) {
        taskId = int.tryParse(entityId);
      }
    }

    return NotificationModel(
      id: id,
      title: data['title']?.toString() ?? 'Notification',
      message: data['message']?.toString() ?? '',
      type: type,
      appointmentId: appointmentId,
      patientFormId: patientFormId,
      documentAccessRequestId: _optionalInt(data['documentAccessRequestId']) ?? _optionalInt(data['document_access_request_id']),
      taskId: taskId,
      treatmentPlanId: treatmentPlanId,
      documentId: documentId,
      chatId: chatId,
      doctorName: data['doctorName']?.toString() ?? data['doctor_name']?.toString(),
      createdAt: createdAt,
      readAt: null,
    );
  }

  static bool _isAppointmentTypeKey(String t) =>
      t.contains('APPOINTMENT') || t == 'APPOINTMENT_REMINDER';
  static bool _isDocumentTypeKey(String t) =>
      t.contains('DOCUMENT') && !t.contains('ACCESS');
  static bool _isChatTypeKey(String t) =>
      t.contains('MESSAGE') || t.contains('CHAT');
  static bool _isTaskTypeKey(String t) =>
      t.contains('TASK');
  static bool _isTreatmentPlanTypeKey(String t) {
    final u = t.toUpperCase();
    if (u.contains('TREATMENT_PLAN')) return true;
    return u == 'INSTALLMENT_DUE_SOON' ||
        u == 'INSTALLMENT_DUE_TODAY' ||
        u == 'INSTALLMENT_OVERDUE' ||
        u == 'INSTALLMENT_SCHEDULE_CREATED';
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'message': message,
      'type': type,
      'appointmentId': appointmentId,
      'patientFormId': patientFormId,
      'documentAccessRequestId': documentAccessRequestId,
      'documentAccessRequestStatus': documentAccessRequestStatus,
      'taskId': taskId,
      'treatmentPlanId': treatmentPlanId,
      'documentId': documentId,
      'chatId': chatId,
      'doctorName': doctorName,
      'createdAt': createdAt.toIso8601String(),
      'readAt': readAt?.toIso8601String(),
    };
  }

  @override
  List<Object?> get props => [
        id,
        title,
        message,
        type,
        appointmentId,
        patientFormId,
        documentAccessRequestId,
        documentAccessRequestStatus,
        taskId,
        treatmentPlanId,
        documentId,
        chatId,
        doctorName,
        createdAt,
        readAt,
      ];
}
