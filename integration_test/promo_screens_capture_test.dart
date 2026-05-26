import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:integration_test/integration_test.dart';
import 'package:shifa_patient_app_v1/app/app.dart';
import 'package:shifa_patient_app_v1/app/router.dart';

/// Captures three PNGs for the Remotion promo.
///
/// Writes to **[monorepo]/promo-video/public/assets/screens/** when the test is run
/// with `Directory.current` = `apps-flutter-patient` (normal `flutter test` from that package).
///
/// Run from `apps-flutter-patient`:
/// `flutter test integration_test/promo_screens_capture_test.dart --dart-define=PROMO_CAPTURE=true`
///
/// Requires a running emulator/device. Uses [PROMO_CAPTURE] (see [AuthNotifier]) so no API login.
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  final binding = IntegrationTestWidgetsFlutterBinding.instance;

  testWidgets('capture promo screens (PROMO_CAPTURE)', (tester) async {
    final container = ProviderContainer();

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: const ShifaPatientApp(),
      ),
    );

    await tester.pumpAndSettle(const Duration(seconds: 8));

    final promoScreensDir = Directory.fromUri(
      Directory.current.absolute.uri.resolve(
        '../promo-video/public/assets/screens/',
      ),
    );

    Future<void> saveShot(String fileName) async {
      await binding.convertFlutterSurfaceToImage();
      final bytes = await binding.takeScreenshot(fileName);
      await promoScreensDir.create(recursive: true);
      await File(
        '${promoScreensDir.path}${Platform.pathSeparator}$fileName',
      ).writeAsBytes(bytes);
    }

    container.read(routerProvider).go('/doctors');
    await tester.pumpAndSettle(const Duration(seconds: 5));
    await saveShot('01-home.png');

    container.read(routerProvider).go('/bookings/create');
    await tester.pumpAndSettle(const Duration(seconds: 5));
    await saveShot('02-booking.png');

    container.read(routerProvider).go('/bookings/promo/waiting');
    await tester.pumpAndSettle(const Duration(seconds: 3));
    await saveShot('03-video.png');
  });
}
