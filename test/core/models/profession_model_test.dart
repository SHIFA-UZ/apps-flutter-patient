import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/models/profession_model.dart';

void main() {
  group('ProfessionData.translate', () {
    test('translates simple profession to Uzbek', () {
      expect(
        ProfessionData.translate('Dentist', 'uz'),
        'Stomatolog',
      );
    });

    test('translates compound backend aliases to Uzbek', () {
      expect(
        ProfessionData.translate('Dentist, Pediatric', 'uz'),
        'Bolalar stomatologi',
      );
      expect(
        ProfessionData.translate('Dentist, Prosthodontist', 'uz'),
        'Ortopedik stomatolog',
      );
      expect(
        ProfessionData.translate('Oral Surgeon, Dentist', 'uz'),
        'Og\'iz bo\'shlig\'i jarrohi',
      );
    });

    test('keeps English for en locale', () {
      expect(
        ProfessionData.translate('Dentist, Pediatric', 'en'),
        'Pediatric Dentist',
      );
    });

    test('sorts by localized label', () {
      final sorted = ProfessionData.sortEnglishKeysForDisplay(
        ['Dentist, Prosthodontist', 'Family Physician', 'Dentist'],
        'uz',
      );
      expect(sorted.first, 'Family Physician');
      expect(sorted, contains('Dentist'));
    });
  });
}
