import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/providers/connectivity_provider.dart';

void main() {
  group('connectivityListOnline', () {
    test('false when only none', () {
      expect(connectivityListOnline([ConnectivityResult.none]), isFalse);
    });

    test('true when wifi present', () {
      expect(connectivityListOnline([ConnectivityResult.wifi]), isTrue);
    });

    test('true when mixed with none and mobile', () {
      expect(
        connectivityListOnline([
          ConnectivityResult.none,
          ConnectivityResult.mobile,
        ]),
        isTrue,
      );
    });
  });
}
