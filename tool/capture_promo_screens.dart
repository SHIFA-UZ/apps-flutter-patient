import 'dart:io';

/// Runs the integration test that saves PNGs under `../promo-video/public/assets/screens/`.
///
/// From repo root:
/// `dart run apps-flutter-patient/tool/capture_promo_screens.dart`
///
/// Or from `apps-flutter-patient`:
/// `dart run tool/capture_promo_screens.dart`
void main() {
  final cwd = Directory.current;
  final isPatientRoot = cwd.path.endsWith('apps-flutter-patient');
  final workingDir =
      isPatientRoot ? cwd.path : '${cwd.path}${Platform.pathSeparator}apps-flutter-patient';

  final result = Process.runSync('flutter', [
    'test',
    'integration_test/promo_screens_capture_test.dart',
    '--dart-define=PROMO_CAPTURE=true',
  ], workingDirectory: workingDir);

  stdout.write(result.stdout);
  stderr.write(result.stderr);

  if (result.exitCode != 0) {
    exit(result.exitCode);
  }

  final promoDir = '${Directory(workingDir).parent.path}${Platform.pathSeparator}promo-video${Platform.pathSeparator}public${Platform.pathSeparator}assets${Platform.pathSeparator}screens';
  stdout.writeln('\nScreenshots: $promoDir${Platform.pathSeparator}');
  stdout.writeln('Remotion: cd ../promo-video && npm run render  (ensure usePngScreens in src/screenAssets.ts).');
}
