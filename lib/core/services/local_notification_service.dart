import 'dart:convert';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

class LocalNotificationService {
  static final LocalNotificationService _instance = LocalNotificationService._internal();
  factory LocalNotificationService() => _instance;
  LocalNotificationService._internal();

  static const int _videoJoinReminderIdBase = 800000;

  final FlutterLocalNotificationsPlugin _notifications = FlutterLocalNotificationsPlugin();
  bool _initialized = false;
  bool _timezoneInitialized = false;
  void Function(Map<String, dynamic>)? _onNotificationTap;

  Future<void> _ensureTimezoneInitialized() async {
    if (_timezoneInitialized) return;
    tz_data.initializeTimeZones();
    try {
      final timeZoneName = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(timeZoneName));
      _timezoneInitialized = true;
    } catch (e) {
      AppLogger.debug('LocalNotificationService: timezone init failed, using local: $e');
      tz.setLocalLocation(tz.local);
      _timezoneInitialized = true;
    }
  }

  Future<void> initialize() async {
    if (_initialized) return;
    await _ensureTimezoneInitialized();

    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    const initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _notifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: (details) {
        // Handle notification tap by routing through the same handler as FCM notifications
        if (details.payload != null && details.payload!.isNotEmpty) {
          try {
            final data = jsonDecode(details.payload!) as Map<String, dynamic>;
            if (_onNotificationTap != null) {
              _onNotificationTap!(data);
            } else {
              AppLogger.debug('LocalNotificationService: No tap handler set, payload: ${details.payload}');
            }
          } catch (e) {
            AppLogger.debug('LocalNotificationService: Error parsing notification payload: $e');
          }
        }
      },
    );

    // Request runtime notification permissions where required.
    final android = _notifications.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    await android?.requestNotificationsPermission();

    final ios = _notifications
        .resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>();
    await ios?.requestPermissions(alert: true, badge: true, sound: true);

    _initialized = true;
  }

  void setOnNotificationTap(void Function(Map<String, dynamic>) callback) {
    _onNotificationTap = callback;
  }

  Future<void> showNotification({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    if (!_initialized) {
      await initialize();
    }

    const androidDetails = AndroidNotificationDetails(
      'appointments_channel',
      'Appointment Notifications',
      channelDescription: 'Notifications for appointment updates',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
    );

    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    const details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _notifications.show(
      id,
      title,
      body,
      details,
      payload: payload,
    );
  }

  Future<void> cancelNotification(int id) async {
    await _notifications.cancel(id);
  }

  Future<void> cancelAllNotifications() async {
    await _notifications.cancelAll();
  }

  /// Schedules a local notification for 5 minutes before the appointment start so the
  /// patient is notified when they can join the video call. No-op if that time is in the past.
  /// [appointmentId] is used for a stable notification id and for the tap payload.
  /// [startAtIso8601] is the appointment start in UTC (e.g. 2026-02-12T13:00:00Z).
  Future<void> scheduleVideoJoinReminder({
    required String appointmentId,
    required String startAtIso8601,
    required String title,
    required String body,
  }) async {
    if (!_initialized) await initialize();
    await _ensureTimezoneInitialized();

    final startUtc = DateTime.parse(startAtIso8601);
    final trigger = startUtc.subtract(const Duration(minutes: 5));
    if (trigger.isBefore(DateTime.now().toUtc())) return;

    final id = _videoJoinReminderIdBase + (appointmentId.hashCode % 100000).abs();
    await _notifications.cancel(id);

    const androidDetails = AndroidNotificationDetails(
      'appointments_channel',
      'Appointment Notifications',
      channelDescription: 'Notifications for appointment updates',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
    );
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    const details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    final scheduledDate = tz.TZDateTime.from(trigger.toLocal(), tz.local);
    final payload = jsonEncode(<String, dynamic>{
      'route': '/bookings/$appointmentId/video',
      'type': 'video_join_ready',
      'appointmentId': appointmentId,
    });

    await _notifications.zonedSchedule(
      id,
      title,
      body,
      scheduledDate,
      details,
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      uiLocalNotificationDateInterpretation:
          UILocalNotificationDateInterpretation.absoluteTime,
      payload: payload,
    );
  }

  /// One-tap care-plan reminder. Schedules a local reminder [after] now (default 1 hour).
  Future<void> scheduleChecklistReminder({
    required String appointmentId,
    required String checklistItem,
    required String title,
    DateTime? scheduledAt,
    Duration after = const Duration(hours: 1),
  }) async {
    if (!_initialized) await initialize();
    await _ensureTimezoneInitialized();

    final trigger = scheduledAt ?? DateTime.now().add(after);
    final id = (appointmentId.hashCode ^ checklistItem.hashCode).abs().remainder(0x7FFFFFFF - 1) + 1;

    const androidDetails = AndroidNotificationDetails(
      'appointments_channel',
      'Appointment Notifications',
      channelDescription: 'Notifications for appointment updates',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
    );
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    const details = NotificationDetails(android: androidDetails, iOS: iosDetails);

    final payload = jsonEncode(<String, dynamic>{
      'route': '/bookings/$appointmentId/visit-summary',
      'type': 'visit_summary_checklist_reminder',
      'appointmentId': appointmentId,
      'item': checklistItem,
    });

    await _notifications.zonedSchedule(
      id,
      title,
      checklistItem,
      tz.TZDateTime.from(trigger, tz.local),
      details,
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      uiLocalNotificationDateInterpretation: UILocalNotificationDateInterpretation.absoluteTime,
      payload: payload,
    );
  }
}
