import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// Centralized notification service (FCM + local). All entry points funnel to a single
/// handler so the app can mark-as-read then navigate in one place.
///
/// Required payload from backend (standardized):
///   - [type] (required): e.g. appointment_reminder, new_document, new_message
///   - [entityId] (required): id for the target (appointment uuid, document id, chat id)
///   - [notificationId] (required): id to mark as read (backend: PUT /notifications/:id/read)
///   - [route] (optional): fallback path if type not mapped
///
/// App states:
///   1. Foreground: onMessage → show local notification (skip if already in read cache)
///   2. Background: onMessageOpenedApp → handleNotificationNavigation(message)
///   3. Terminated: getInitialMessage stored → processPendingInitialMessage() → handleNotificationNavigation
///   4. Local notification tap → handleNotificationNavigationFromData(payload)
class PushNotificationService {
  static final PushNotificationService _instance = PushNotificationService._internal();
  factory PushNotificationService() => _instance;
  PushNotificationService._internal();

  final FirebaseMessaging _firebaseMessaging = FirebaseMessaging.instance;
  final FlutterLocalNotificationsPlugin _localNotifications = FlutterLocalNotificationsPlugin();

  String? _fcmToken;
  void Function(Map<String, dynamic>)? _onNotificationTap;
  void Function(String token)? _onFcmTokenReady;
  RemoteMessage? _pendingInitialMessage;
  Map<String, dynamic>? _pendingLocalNotificationTap;

  /// IDs we've already marked read or are about to handle – avoid duplicate popups in foreground.
  final Set<String> _readNotificationIds = {};

  /// Initialize: wire all three FCM entry points. Foreground shows local; background and
  /// terminated both use [handleNotificationNavigation].
  Future<void> initialize() async {
    final settings = await _firebaseMessaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
    );

    if (settings.authorizationStatus != AuthorizationStatus.authorized) return;

    await _initializeLocalNotifications();

    _fcmToken = await _firebaseMessaging.getToken();
    if (kDebugMode) debugPrint('FCM Token: $_fcmToken');
    if (_fcmToken != null) _onFcmTokenReady?.call(_fcmToken!);
    _firebaseMessaging.onTokenRefresh.listen((token) {
      _fcmToken = token;
      if (kDebugMode) debugPrint('FCM Token refreshed');
      _onFcmTokenReady?.call(token);
    });

    // 1. Foreground: show local notification only if not already in read cache
    FirebaseMessaging.onMessage.listen(_handleForegroundMessage);

    // 2. Background: user tapped notification → single handler
    FirebaseMessaging.onMessageOpenedApp.listen(handleNotificationNavigation);

    // 3. Terminated: store; delivered when callback is set via processPendingInitialMessage()
    final initialMessage = await _firebaseMessaging.getInitialMessage();
    if (initialMessage != null && initialMessage.data.isNotEmpty) {
      _pendingInitialMessage = initialMessage;
    }
  }

  /// Single entry for navigation flow (FCM background + terminated). App layer must
  /// mark notification as read first, then navigate by type.
  void handleNotificationNavigation(RemoteMessage message) {
    if (message.data.isEmpty) return;
    _deliverPayload(Map<String, dynamic>.from(message.data));
  }

  /// Same as [handleNotificationNavigation] but from a data map (e.g. local notification tap).
  void handleNotificationNavigationFromData(Map<String, dynamic> data) {
    if (data.isEmpty) return;
    _deliverPayload(data);
  }

  void _deliverPayload(Map<String, dynamic> data) {
    final notificationId = _notificationIdFromPayload(data);
    if (notificationId != null && notificationId.isNotEmpty) {
      _readNotificationIds.add(notificationId);
    }
    if (_onNotificationTap != null) {
      _onNotificationTap!(data);
    } else {
      _pendingLocalNotificationTap = data;
    }
  }

  static String? _notificationIdFromPayload(Map<String, dynamic> data) {
    final v = data['notificationId'] ?? data['id'];
    if (v == null) return null;
    return v.toString();
  }

  /// Call after setting the tap callback (e.g. when MainShell is ready). Delivers
  /// any notification that opened the app from terminated state or local tap; both
  /// use the same [handleNotificationNavigation] path.
  void processPendingInitialMessage() {
    if (_onNotificationTap == null) return;
    final fcmPending = _pendingInitialMessage;
    if (fcmPending != null && fcmPending.data.isNotEmpty) {
      _pendingInitialMessage = null;
      handleNotificationNavigation(fcmPending);
      return;
    }
    final localPending = _pendingLocalNotificationTap;
    if (localPending != null && localPending.isNotEmpty) {
      _pendingLocalNotificationTap = null;
      handleNotificationNavigationFromData(localPending);
    }
  }

  Future<void> _initializeLocalNotifications() async {
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );
    const initSettings = InitializationSettings(android: androidSettings, iOS: iosSettings);

    await _localNotifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: (details) {
        if (details.payload == null || details.payload!.isEmpty) return;
        try {
          final data = Map<String, dynamic>.from(jsonDecode(details.payload!) as Map<String, dynamic>);
          handleNotificationNavigationFromData(data);
        } catch (e) {
          if (kDebugMode) debugPrint('Error parsing notification payload: $e');
        }
      },
    );
  }

  /// Foreground: show local notification only if not already in read cache (no duplicate popup).
  Future<void> _handleForegroundMessage(RemoteMessage message) async {
    final data = Map<String, dynamic>.from(message.data);
    final notificationId = _notificationIdFromPayload(data);
    if (notificationId != null && _readNotificationIds.contains(notificationId)) {
      return;
    }
    await _showLocalNotification(
      title: message.notification?.title ?? 'New Notification',
      body: message.notification?.body ?? '',
      data: data,
    );
  }

  Future<void> _showLocalNotification({
    required String title,
    required String body,
    Map<String, dynamic>? data,
  }) async {
    const androidDetails = AndroidNotificationDetails(
      'shifa_patient_channel',
      'Shifa Patient Notifications',
      channelDescription: 'Notifications for messages, appointments, and tasks',
      importance: Importance.high,
      priority: Priority.high,
      showWhen: true,
    );
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    const notificationDetails = NotificationDetails(android: androidDetails, iOS: iosDetails);

    final payload = (data != null && data.isNotEmpty) ? jsonEncode(data) : null;
    var id = 0;
    if (data != null) {
      final nid = data['notificationId'] ?? data['id'];
      if (nid != null) {
        if (nid is int) id = nid; else id = int.tryParse(nid.toString()) ?? 0;
      }
    }
    if (id == 0) id = DateTime.now().millisecondsSinceEpoch.remainder(100000);
    await _localNotifications.show(
      id.abs().clamp(1, 0x7FFFFFFF),
      title,
      body,
      notificationDetails,
      payload: payload,
    );
  }

  void setOnNotificationTap(void Function(Map<String, dynamic>) callback) {
    _onNotificationTap = callback;
  }

  void setOnFcmTokenReady(void Function(String token) callback) {
    _onFcmTokenReady = callback;
    if (_fcmToken != null) callback(_fcmToken!);
  }

  String? getFcmToken() => _fcmToken;

  Future<void> subscribeToTopic(String topic) async =>
      _firebaseMessaging.subscribeToTopic(topic);
  Future<void> unsubscribeFromTopic(String topic) async =>
      _firebaseMessaging.unsubscribeFromTopic(topic);
}
