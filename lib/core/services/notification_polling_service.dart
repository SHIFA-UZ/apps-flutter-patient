import 'dart:async';
import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/providers/language_provider.dart';
import 'package:shifa_patient_app_v1/core/services/local_notification_service.dart';
import 'package:shifa_patient_app_v1/features/notifications/providers/notifications_provider.dart';
import 'package:shifa_patient_app_v1/features/notifications/utils/notification_localization.dart';

class NotificationPollingService {
  Timer? _pollingTimer;
  final Ref ref;
  final LocalNotificationService _localNotifications = LocalNotificationService();
  Set<int> _shownNotificationIds = {};
  static const String _shownNotificationsKey = 'shown_notification_ids';
  bool _isLoadingShownIds = false;

  NotificationPollingService(this.ref);

  void startPolling({Duration interval = const Duration(seconds: 30)}) {
    // Initialize local notifications
    _localNotifications.initialize();

    // Stop any existing timer
    stopPolling();

    // Load shown notification IDs from persistent storage
    _loadShownNotificationIds();

    // Start polling
    _pollingTimer = Timer.periodic(interval, (_) async {
      await _checkForNewNotifications();
    });

    // Check after a short delay to allow shown IDs to load
    Future.delayed(const Duration(milliseconds: 500), () {
      _checkForNewNotifications();
    });
  }

  void stopPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  Future<void> _loadShownNotificationIds() async {
    if (_isLoadingShownIds) return;
    _isLoadingShownIds = true;

    try {
      final prefs = await SharedPreferences.getInstance();
      final stored = prefs.getString(_shownNotificationsKey);
      if (stored != null && stored.isNotEmpty) {
        final List<dynamic> decoded = jsonDecode(stored);
        _shownNotificationIds = decoded.map((e) => e as int).toSet();
      }
    } catch (e) {
      AppLogger.error('Error loading shown notification IDs:', e);
      _shownNotificationIds = {};
    } finally {
      _isLoadingShownIds = false;
    }
  }

  Future<void> _saveShownNotificationIds() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final encoded = jsonEncode(_shownNotificationIds.toList());
      await prefs.setString(_shownNotificationsKey, encoded);
    } catch (e) {
      AppLogger.error('Error saving shown notification IDs:', e);
    }
  }

  Future<void> _checkForNewNotifications() async {
    // Wait for shown IDs to load if still loading
    if (_isLoadingShownIds) {
      await Future.delayed(const Duration(milliseconds: 100));
      if (_isLoadingShownIds) return; // Still loading, skip this check
    }

    try {
      final repository = ref.read(notificationsRepositoryProvider);
      final notifications = await repository.getNotifications();

      // Filter for unread notifications that haven't been shown yet
      final newNotifications = notifications
          .where((n) => !n.isRead && !_shownNotificationIds.contains(n.id))
          .toList();

      if (newNotifications.isEmpty) return;

      for (final notification in newNotifications) {
        // Show in-app alert dialog for the first new notification only (dialog is shown by MainShell)
        if (notification.id == newNotifications.first.id) {
          ref.read(pendingNotificationDialogProvider.notifier).show(notification);
        }

        // Also show system tray notification with localized title/body
        final locale = ref.read(languageProvider).locale;
        final l10n = AppLocalizations(locale);
        final title = NotificationLocalization.getTitle(notification, l10n);
        final body = NotificationLocalization.getMessage(notification, l10n);
        await _localNotifications.showNotification(
          id: notification.id,
          title: title,
          body: body,
          payload: jsonEncode({
            'notificationId': notification.id,
            'type': notification.type,
            'entityId': notification.appointmentId?.toString() ??
                       notification.documentId ??
                       notification.chatId ??
                       notification.taskId?.toString(),
            'appointmentId': notification.appointmentId,
            'documentId': notification.documentId,
            'chatId': notification.chatId,
            'taskId': notification.taskId,
          }),
        );

        // Mark as shown in memory
        _shownNotificationIds.add(notification.id);
      }

      // Persist shown notification IDs
      await _saveShownNotificationIds();

      // Clean up old notification IDs (keep only last 100)
      if (_shownNotificationIds.length > 100) {
        final allIds = notifications.map((n) => n.id).toSet();
        _shownNotificationIds = _shownNotificationIds.intersection(allIds);
        await _saveShownNotificationIds();
      }
    } catch (e) {
      AppLogger.error('Error checking notifications:', e);
    }
  }

  Future<void> markNotificationAsShown(int notificationId) async {
    _shownNotificationIds.add(notificationId);
    await _saveShownNotificationIds();
  }

  /// Clear shown notification IDs (useful when user logs out)
  Future<void> clearShownNotifications() async {
    _shownNotificationIds.clear();
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_shownNotificationsKey);
    } catch (e) {
      AppLogger.error('Error clearing shown notification IDs:', e);
    }
  }
}

final notificationPollingServiceProvider = Provider<NotificationPollingService>((ref) {
  return NotificationPollingService(ref);
});
