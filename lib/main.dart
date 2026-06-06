import 'dart:async';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:shifa_patient_app_v1/app/app.dart';
import 'package:shifa_patient_app_v1/core/services/push_notification_service.dart';
import 'package:shifa_patient_app_v1/core/utils/storage_service.dart';
import 'package:shifa_patient_app_v1/firebase_options.dart';

/// Background message handler (must be top-level function)
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  AppLogger.debug('Background message: ${message.messageId}');
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Only status bar transparent; navigation bar keeps system default (native color)
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: Colors.transparent,
  ));

  // Clear Keychain on first launch after reinstall (iOS Keychain persists across uninstalls)
  try {
    final prefs = await SharedPreferences.getInstance();
    if (prefs.getBool('has_launched_before') != true) {
      await StorageService()
          .clearAuthToken()
          .timeout(const Duration(seconds: 3), onTimeout: () {});
      await prefs.setBool('has_launched_before', true);
    }
  } catch (e, st) {
    AppLogger.error('First-launch storage cleanup skipped:', e, st);
  }

  try {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    ).timeout(const Duration(seconds: 10));
    if (!kIsWeb) {
      FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
    }
  } catch (e, st) {
    AppLogger.error('Firebase/FCM init skipped (run "dart run flutterfire configure" if you need push):', e, st);
  }

  runApp(
    const ProviderScope(
      child: ShifaPatientApp(),
    ),
  );

  // Defer push permission/token setup so Android leaves the native splash promptly.
  if (!kIsWeb) {
    unawaited(
      PushNotificationService().initialize().catchError((Object e, StackTrace st) {
        AppLogger.error('Deferred FCM init failed:', e, st);
      }),
    );
  }
}
