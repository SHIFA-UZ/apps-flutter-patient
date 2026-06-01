// ignore: avoid_web_libraries_in_flutter
import 'dart:html' as html;
// ignore: avoid_web_libraries_in_flutter
import 'dart:js_util' as js_util;

import 'package:flutter/foundation.dart';

Future<void> showBrowserNotification({
  required String title,
  required String body,
  required String tag,
  required void Function() onClick,
}) async {
  if (!html.Notification.supported) {
    if (kDebugMode) {
      debugPrint('Browser Notification API not supported');
    }
    return;
  }
  if (html.Notification.permission != 'granted') {
    final perm = await html.Notification.requestPermission();
    if (perm != 'granted') {
      if (kDebugMode) {
        debugPrint('Web notification permission not granted: $perm');
      }
      return;
    }
  }
  final notification = html.Notification(
    title,
    body: body,
    icon: '/icons/Icon-192.png',
    tag: tag,
  );
  notification.onClick.listen((_) {
    try {
      js_util.callMethod(html.window, 'focus', []);
    } catch (_) {}
    onClick();
  });
}
