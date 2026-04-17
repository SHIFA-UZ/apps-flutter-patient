import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/app/app.dart';

/// Golden-path smoke: ensures the app widget tree mounts under the integration binding.
///
/// Full E2E (login → book → document → video → chat) requires a running backend,
/// Firebase configuration, and device permissions; run those on staging with a
/// dedicated patrol/integration suite when available.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('patient app mounts (integration binding)', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: ShifaPatientApp()));
    await tester.pump();
    expect(find.byType(ShifaPatientApp), findsOneWidget);
    // Allow async router / first frame without hanging forever on CI
    await tester.pump(const Duration(seconds: 2));
  });
}
