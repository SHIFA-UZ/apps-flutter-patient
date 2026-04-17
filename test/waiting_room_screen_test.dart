import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/waiting_room_screen.dart';

void main() {
  testWidgets('WaitingRoomScreen shows join control', (tester) async {
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (_, __) => const WaitingRoomScreen(appointmentId: '42'),
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp.router(
        routerConfig: router,
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
        ],
        supportedLocales: const [
          Locale('en'),
          Locale('de'),
          Locale('uz'),
          Locale('ru'),
        ],
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byType(WaitingRoomScreen), findsOneWidget);
    expect(find.byType(ElevatedButton), findsOneWidget);
  });
}
