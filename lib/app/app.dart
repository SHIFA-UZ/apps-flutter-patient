import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/providers/language_provider.dart';
import 'package:shifa_patient_app_v1/core/services/notification_polling_service.dart';
import 'package:shifa_patient_app_v1/core/services/push_notification_service.dart';
import 'package:shifa_patient_app_v1/features/notifications/providers/notifications_provider.dart';
import 'package:shifa_patient_app_v1/core/theme/app_theme.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_lock_lifecycle_layer.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

class ShifaPatientApp extends ConsumerStatefulWidget {
  const ShifaPatientApp({super.key});

  @override
  ConsumerState<ShifaPatientApp> createState() => _ShifaPatientAppState();
}

class _ShifaPatientAppState extends ConsumerState<ShifaPatientApp>
    with WidgetsBindingObserver {
  bool _callbackSetup = false;
  bool _fcmTokenUploadSetup = false;
  bool _clearedNotificationsOnLogout = false;
  bool _timezoneSyncOnAuthDone = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      final authState = ref.read(authStateProvider);
      if (authState.isAuthenticated) {
        ref.read(profileProvider.notifier).syncTimeZoneFromDevice();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    try {
      final router = ref.watch(routerProvider);
      final languageState = ref.watch(languageProvider);
      final authState = ref.watch(authStateProvider);
    
    // Set up API client auth error callback once
    if (!_callbackSetup) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        final apiClient = ref.read(apiClientProvider);
        final authNotifier = ref.read(authStateProvider.notifier);
        
        apiClient.setOnAuthError(() {
          // Only logout if currently authenticated to prevent loops
          final currentAuthState = ref.read(authStateProvider);
          if (currentAuthState.isAuthenticated) {
            authNotifier.logout();
          }
        });
        
        setState(() {
          _callbackSetup = true;
        });
      });
    }

    // Rely on FCM push for notifications; polling disabled to save battery/network.
    // (NotificationPollingService remains available if needed for foreground-only fallback.)
    // Sync device timezone to backend once when user is authenticated (silent, no UI)
    if (authState.isAuthenticated && !_timezoneSyncOnAuthDone) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _timezoneSyncOnAuthDone = true;
        ref.read(profileProvider.notifier).syncTimeZoneFromDevice();
      });
    }
    if (!authState.isAuthenticated) {
      _timezoneSyncOnAuthDone = false;
    }
    // Only use FCM when Firebase was initialized (run "dart run flutterfire configure" for push)
    if (authState.isAuthenticated && !_fcmTokenUploadSetup && Firebase.apps.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _fcmTokenUploadSetup = true;
        final pushService = PushNotificationService();
        final repository = ref.read(notificationsRepositoryProvider);
        pushService.setOnFcmTokenReady((token) {
          if (token.isEmpty) return;
          repository.updateFcmToken(token).then((_) {
            AppLogger.debug('FCM token uploaded to backend');
          }).catchError((e) {
            AppLogger.error('FCM token upload failed:', e);
          });
        });
        final token = pushService.getFcmToken();
        if (token != null && token.isNotEmpty) {
          repository.updateFcmToken(token).then((_) {
            AppLogger.debug('FCM token uploaded to backend');
          }).catchError((e) {
            AppLogger.error('FCM token upload failed:', e);
          });
        }
      });
    } else if (!authState.isAuthenticated) {
      _fcmTokenUploadSetup = false;
      if (!_clearedNotificationsOnLogout) {
        _clearedNotificationsOnLogout = true;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          ref.read(notificationPollingServiceProvider).clearShownNotifications();
        });
      }
    } else {
      _clearedNotificationsOnLogout = false;
    }

    return AppLockLifecycleLayer(
      child: MaterialApp.router(
        title: AppLocalizations(languageState.locale).appName,
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        routerConfig: router,
        locale: languageState.locale,
        supportedLocales: const [
          Locale('en'), // English
          Locale('de'), // German
          Locale('uz'), // Uzbek
          Locale('ru'), // Russian
        ],
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
      ),
    );
    } catch (e, stackTrace) {
      AppLogger.error('Error in ShifaPatientApp build:', e, stackTrace);
      // Return a simple error screen if there's a critical error
      return MaterialApp(
        home: Scaffold(
          backgroundColor: Colors.white,
          body: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.error_outline, size: 64, color: Colors.red),
                const SizedBox(height: 16),
                Text(
                '${(AppLocalizations.of(context) ?? AppLocalizations(const Locale('en'))).translate('appInitializationError')}: '
                '${userFriendlyError(AppLocalizations.of(context) ?? AppLocalizations(const Locale('en')), e, logContext: 'App init')}',
              ),
                const SizedBox(height: 8),
                Text(
                  (AppLocalizations.of(context) ?? AppLocalizations(const Locale('en'))).translate('pleaseRestartApp'),
                  style: const TextStyle(fontSize: 12),
                ),
              ],
            ),
          ),
        ),
      );
    }
  }
}
