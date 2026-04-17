import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/core/models/notification_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/services/local_notification_service.dart';
import 'package:shifa_patient_app_v1/features/notifications/data/notifications_repository.dart';

/// Pending notification to show in an in-app alert dialog (e.g. when polling finds a new one).
/// When non-null, the shell shows a dialog; on "View" we navigate to the reason, on "Close" we just dismiss.
class PendingNotificationDialogNotifier extends StateNotifier<NotificationModel?> {
  PendingNotificationDialogNotifier() : super(null);

  void show(NotificationModel notification) {
    state = notification;
  }

  void clear() {
    state = null;
  }
}

final pendingNotificationDialogProvider =
    StateNotifierProvider<PendingNotificationDialogNotifier, NotificationModel?>((ref) {
  return PendingNotificationDialogNotifier();
});

/// Pending FCM/local notification tap payload (set by PushNotificationService, consumed by MainShell).
class PendingNotificationTapNotifier extends StateNotifier<Map<String, dynamic>?> {
  PendingNotificationTapNotifier() : super(null);

  void setPending(Map<String, dynamic> data) {
    state = data;
  }

  void clear() {
    state = null;
  }
}

final pendingNotificationTapProvider =
    StateNotifierProvider<PendingNotificationTapNotifier, Map<String, dynamic>?>((ref) {
  return PendingNotificationTapNotifier();
});

final notificationsRepositoryProvider = Provider<NotificationsRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return NotificationsRepository(apiClient);
});

final localNotificationServiceProvider = Provider<LocalNotificationService>((ref) {
  return LocalNotificationService();
});

final notificationsProvider = FutureProvider<List<NotificationModel>>((ref) async {
  final repository = ref.watch(notificationsRepositoryProvider);
  return repository.getNotifications();
});

final unreadNotificationsCountProvider = FutureProvider<int>((ref) async {
  final repository = ref.watch(notificationsRepositoryProvider);
  return repository.getUnreadCount();
});

final notificationsControllerProvider = Provider<NotificationsController>((ref) {
  return NotificationsController(ref);
});

/// Request IDs we've already approved or rejected this session; buttons are hidden for these.
final documentAccessRequestActedIdsProvider = StateProvider<Set<int>>((ref) => {});

class NotificationsController {
  final Ref ref;

  NotificationsController(this.ref);

  Future<void> markAsRead(int notificationId) async {
    final repository = ref.read(notificationsRepositoryProvider);
    await repository.markAsRead(notificationId);
    ref.invalidate(notificationsProvider);
    ref.invalidate(unreadNotificationsCountProvider);

    // Cancel the local notification from system tray
    try {
      final localNotificationService = ref.read(localNotificationServiceProvider);
      await localNotificationService.cancelNotification(notificationId);
    } catch (e) {
      debugPrint('Failed to cancel local notification: $e');
    }
  }

  /// Mark as read by string id (e.g. uuid from FCM payload). Does not block on failure.
  Future<void> markAsReadById(String notificationId) async {
    try {
      final repository = ref.read(notificationsRepositoryProvider);
      await repository.markAsReadById(notificationId);
      ref.invalidate(notificationsProvider);
      ref.invalidate(unreadNotificationsCountProvider);
    } catch (e) {
      AppLogger.error('markAsReadById failed:', e);
    }
  }

  Future<void> markAllAsRead() async {
    final repository = ref.read(notificationsRepositoryProvider);
    await repository.markAllAsRead();
    ref.invalidate(notificationsProvider);
    ref.invalidate(unreadNotificationsCountProvider);
  }

  Future<void> refresh() async {
    ref.invalidate(notificationsProvider);
    ref.invalidate(unreadNotificationsCountProvider);
  }

  Future<void> approveDocumentAccessRequest(int requestId, int notificationId) async {
    ref.read(documentAccessRequestActedIdsProvider.notifier).update((s) => {...s, requestId});
    try {
      final repository = ref.read(notificationsRepositoryProvider);
      await repository.approveDocumentAccessRequest(requestId);
      await markAsRead(notificationId);
    } catch (e) {
      ref.read(documentAccessRequestActedIdsProvider.notifier).update((s) => {...s}..remove(requestId));
      rethrow;
    }
  }

  Future<void> rejectDocumentAccessRequest(int requestId, int notificationId) async {
    ref.read(documentAccessRequestActedIdsProvider.notifier).update((s) => {...s, requestId});
    try {
      final repository = ref.read(notificationsRepositoryProvider);
      await repository.rejectDocumentAccessRequest(requestId);
      await markAsRead(notificationId);
    } catch (e) {
      ref.read(documentAccessRequestActedIdsProvider.notifier).update((s) => {...s}..remove(requestId));
      rethrow;
    }
  }
}
