import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/features/notifications/presentation/screens/notifications_screen.dart';
import 'package:shifa_patient_app_v1/features/notifications/providers/notifications_provider.dart';

void main() {
  group('NotificationsScreen AppBar', () {
    testWidgets('AppBar title uses LayoutBuilder for responsive title', (
      WidgetTester tester,
    ) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [notificationsProvider.overrideWith((ref) async => [])],
          child: MaterialApp(
            localizationsDelegates: const [
              AppLocalizations.delegate,
              GlobalMaterialLocalizations.delegate,
              GlobalWidgetsLocalizations.delegate,
              GlobalCupertinoLocalizations.delegate,
            ],
            supportedLocales: const [Locale('en'), Locale('uz'), Locale('ru')],
            locale: const Locale('en'),
            home: const NotificationsScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final appBar = tester.widget<AppBar>(find.byType(AppBar));
      expect(appBar.title, isA<LayoutBuilder>());
      expect(find.text('Notifications'), findsOneWidget);
    });

    testWidgets('AppBar title uses localized string (English)', (
      WidgetTester tester,
    ) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [notificationsProvider.overrideWith((ref) async => [])],
          child: MaterialApp(
            localizationsDelegates: const [
              AppLocalizations.delegate,
              GlobalMaterialLocalizations.delegate,
              GlobalWidgetsLocalizations.delegate,
              GlobalCupertinoLocalizations.delegate,
            ],
            supportedLocales: const [Locale('en'), Locale('uz'), Locale('ru')],
            locale: const Locale('en'),
            home: const NotificationsScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Notifications'), findsOneWidget);
    });
  });
}
