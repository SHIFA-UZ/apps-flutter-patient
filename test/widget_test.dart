import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:shifa_patient_app_v1/app/app.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const ProviderScope(child: ShifaPatientApp()));

    // Verify that the app builds without crashing
    expect(find.byType(ShifaPatientApp), findsOneWidget);
  });
}
