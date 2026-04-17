import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/models/notification_model.dart';
import 'package:shifa_patient_app_v1/features/notifications/services/notification_tap_handler.dart';

void main() {
  group('NotificationTapHandler.getTargetLocation', () {
    test(
      'SIGNATURE_REQUESTED with appointmentId returns appointment details route',
      () {
        final n = NotificationModel(
          id: 1,
          title: 'Signature Requested',
          message: 'Please sign',
          type: 'SIGNATURE_REQUESTED',
          appointmentId: 42,
          createdAt: DateTime.now(),
        );
        expect(
          NotificationTapHandler.getTargetLocation(n),
          '${AppRoutes.bookings}/42',
        );
      },
    );

    test('SIGNATURE_REQUESTED without appointmentId returns null', () {
      final n = NotificationModel(
        id: 1,
        title: 'Signature Requested',
        message: 'Please sign',
        type: 'SIGNATURE_REQUESTED',
        appointmentId: null,
        createdAt: DateTime.now(),
      );
      expect(NotificationTapHandler.getTargetLocation(n), isNull);
    });

    test(
      'title contains "signature" (any type) with appointmentId returns appointment details route',
      () {
        final n = NotificationModel(
          id: 1,
          title: 'Signature Requested',
          message: 'Dr. X requests signature',
          type: 'OTHER',
          appointmentId: 10,
          createdAt: DateTime.now(),
        );
        expect(
          NotificationTapHandler.getTargetLocation(n),
          '${AppRoutes.bookings}/10',
        );
      },
    );

    test(
      'APPOINTMENT_REMINDER with appointmentId returns appointment details route',
      () {
        final n = NotificationModel(
          id: 1,
          title: 'Reminder',
          message: 'You have an appointment',
          type: 'APPOINTMENT_REMINDER',
          appointmentId: 99,
          createdAt: DateTime.now(),
        );
        expect(
          NotificationTapHandler.getTargetLocation(n),
          '${AppRoutes.bookings}/99',
        );
      },
    );

    test('APPOINTMENT_REMINDER without appointmentId returns null', () {
      final n = NotificationModel(
        id: 1,
        title: 'Reminder',
        message: 'You have an appointment',
        type: 'APPOINTMENT_REMINDER',
        appointmentId: null,
        createdAt: DateTime.now(),
      );
      expect(NotificationTapHandler.getTargetLocation(n), isNull);
    });

    test(
      'NEW_APPOINTMENT with appointmentId returns appointment details route',
      () {
        final n = NotificationModel(
          id: 1,
          title: 'New Appointment',
          message: 'Scheduled',
          type: 'NEW_APPOINTMENT',
          appointmentId: 5,
          createdAt: DateTime.now(),
        );
        expect(
          NotificationTapHandler.getTargetLocation(n),
          '${AppRoutes.bookings}/5',
        );
      },
    );

    test('DOCUMENT_ACCESS_REQUEST returns null (no tap navigation)', () {
      final n = NotificationModel(
        id: 1,
        title: 'Document Access',
        message: 'Request',
        type: 'DOCUMENT_ACCESS_REQUEST',
        documentAccessRequestId: 7,
        createdAt: DateTime.now(),
      );
      expect(NotificationTapHandler.getTargetLocation(n), isNull);
    });

    test('notification with documentId returns document details route', () {
      final n = NotificationModel(
        id: 1,
        title: 'Document',
        message: 'Shared',
        type: 'DOCUMENT_SHARED',
        documentId: 'doc-123',
        createdAt: DateTime.now(),
      );
      expect(
        NotificationTapHandler.getTargetLocation(n),
        '${AppRoutes.documents}/doc-123',
      );
    });

    test('NEW_MESSAGE with chatId returns chat route', () {
      final n = NotificationModel(
        id: 1,
        title: 'New message',
        message: 'From doctor',
        type: 'NEW_MESSAGE',
        chatId: 'chat-456',
        createdAt: DateTime.now(),
      );
      expect(NotificationTapHandler.getTargetLocation(n), AppRoutes.chat);
    });

    test(
      'APPOINTMENT_UPCOMING / appointment close with appointmentId returns appointment details',
      () {
        final n = NotificationModel(
          id: 1,
          title: 'Your appointment is close',
          message: 'Appointment soon',
          type: 'APPOINTMENT_UPCOMING',
          appointmentId: 7,
          createdAt: DateTime.now(),
        );
        expect(
          NotificationTapHandler.getTargetLocation(n),
          '${AppRoutes.bookings}/7',
        );
      },
    );

    test(
      'unknown type with appointmentId returns appointment details (fallback)',
      () {
        final n = NotificationModel(
          id: 1,
          title: 'Your appointment is close',
          message: 'Tomorrow at 10am',
          type: 'INFO',
          appointmentId: 3,
          createdAt: DateTime.now(),
        );
        expect(
          NotificationTapHandler.getTargetLocation(n),
          '${AppRoutes.bookings}/3',
        );
      },
    );
  });
}
