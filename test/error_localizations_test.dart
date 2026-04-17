import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';

void main() {
  group('translateError', () {
    test('maps known backend message to localized string', () {
      final l10nUz = AppLocalizations(const Locale('uz'));
      final out = translateError(l10nUz, 'Invalid credentials');
      expect(out, isNotEmpty);
      expect(out, isNot('Invalid credentials'));
    });

    test('empty input returns generic', () {
      final l10n = AppLocalizations(const Locale('en'));
      final out = translateError(l10n, '');
      expect(out, isNotEmpty);
    });

    test('technical SQL-like message returns generic', () {
      final l10n = AppLocalizations(const Locale('en'));
      final out = translateError(l10n, 'SELECT * FROM patients');
      expect(out, isNot(contains('SELECT')));
    });

    test('prefix Network error maps with placeholder', () {
      final l10n = AppLocalizations(const Locale('en'));
      final out = translateError(l10n, 'Network error: timeout');
      expect(out, isNotEmpty);
      expect(
        out.toLowerCase(),
        anyOf(contains('network'), contains('timeout')),
      );
    });
  });

  group('userFriendlyError', () {
    test('returns localized message for Exception', () {
      final l10n = AppLocalizations(const Locale('en'));
      final out = userFriendlyError(l10n, Exception('Invalid credentials'));
      expect(out, isNotEmpty);
    });
  });
}
