import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/app/persistent_bottom_bar.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/notification_model.dart';
import 'package:shifa_patient_app_v1/core/services/push_notification_service.dart';
import 'package:shifa_patient_app_v1/core/services/local_notification_service.dart';
import 'package:shifa_patient_app_v1/features/notifications/providers/notifications_provider.dart';
import 'package:shifa_patient_app_v1/features/notifications/services/notification_tap_handler.dart';
import 'package:shifa_patient_app_v1/features/notifications/utils/notification_localization.dart';

class MainShell extends ConsumerStatefulWidget {
  final StatefulNavigationShell navigationShell;

  const MainShell({super.key, required this.navigationShell});

  @override
  ConsumerState<MainShell> createState() => _MainShellState();
}

class _MainShellState extends ConsumerState<MainShell> {
  bool _pushTapCallbackSet = false;
  bool _localNotificationCallbackSet = false;

  @override
  Widget build(BuildContext context) {
    // Set up FCM notification tap handler
    if (!_pushTapCallbackSet && Firebase.apps.isNotEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _pushTapCallbackSet = true;
        final pushService = PushNotificationService();
        pushService.setOnNotificationTap((data) {
          ref.read(pendingNotificationTapProvider.notifier).setPending(data);
        });
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted) return;
          pushService.processPendingInitialMessage();
        });
      });
    }

    // Set up local notification tap handler (for polling service notifications)
    if (!_localNotificationCallbackSet) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        _localNotificationCallbackSet = true;
        final localNotificationService = LocalNotificationService();
        localNotificationService.setOnNotificationTap((data) {
          ref.read(pendingNotificationTapProvider.notifier).setPending(data);
        });
      });
    }

    ref.listen<Map<String, dynamic>?>(pendingNotificationTapProvider, (previous, data) {
      if (data == null || data.isEmpty || !context.mounted) return;
      final notifier = ref.read(pendingNotificationTapProvider.notifier);
      final controller = ref.read(notificationsControllerProvider);
      final l10n = AppLocalizations.of(context);
      if (l10n == null) {
        notifier.clear();
        return;
      }
      NotificationTapHandler.handleTapFromPayload(
        context: context,
        data: data,
        controller: controller,
        translate: l10n.translate,
      );
      notifier.clear();
    });

    ref.listen<NotificationModel?>(pendingNotificationDialogProvider, (previous, next) {
      if (next != null && context.mounted) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!context.mounted) return;
          _showNotificationDialog(context, next);
        });
      }
    });

    return Scaffold(
      body: widget.navigationShell,
      resizeToAvoidBottomInset: true,
      bottomNavigationBar: PersistentBottomBar(
        currentIndex: widget.navigationShell.currentIndex,
        navigationShell: widget.navigationShell,
      ),
    );
  }

  Future<void> _showNotificationDialog(BuildContext context, NotificationModel notification) async {
    final controller = ref.read(notificationsControllerProvider);
    final dialogNotifier = ref.read(pendingNotificationDialogProvider.notifier);

    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        final l10n = AppLocalizations.of(ctx)!;
        final title = NotificationLocalization.getTitle(notification, l10n);
        final message = NotificationLocalization.getMessage(notification, l10n);
        return AlertDialog(
        title: Text(title),
        content: SingleChildScrollView(
          child: Text(message),
        ),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              dialogNotifier.clear();
            },
            child: Text(l10n.close),
          ),
          FilledButton(
            onPressed: () async {
              await NotificationTapHandler.handleTap(
                context: ctx,
                notification: notification,
                markAsRead: controller.markAsRead,
                translate: l10n.translate,
              );
              if (ctx.mounted) Navigator.of(ctx).pop();
              dialogNotifier.clear();
              if (notification.isDocumentAccessRequest && context.mounted) {
                context.go(AppRoutes.notifications);
              }
            },
            child: Text(l10n.translate('view')),
          ),
        ],
      );
      },
    );
    dialogNotifier.clear();
  }
}
