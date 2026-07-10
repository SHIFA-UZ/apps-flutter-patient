import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/models/region_catalog.dart';
import 'package:shifa_patient_app_v1/features/doctors/data/doctors_filter_catalog.dart';

void main() {
  group('DoctorsFilterCatalog.regionOptions', () {
    test('includes Uzbekistan viloyats by default', () {
      final options = DoctorsFilterCatalog.regionOptions(const []);
      expect(options, contains('Toshkent shahri'));
      expect(options, contains('Samarqand viloyati'));
      expect(options.length, greaterThanOrEqualTo(14));
    });

    test('merges international city from doctor data', () {
      final options = DoctorsFilterCatalog.regionOptions([
        const DoctorModel(
          id: '1',
          firstName: 'A',
          lastName: 'B',
          fullName: 'A B',
          region: 'Berlin',
        ),
      ]);
      expect(options, contains('Berlin'));
      expect(options, contains('Toshkent shahri'));
    });

    test('maps doctor city to canonical viloyat', () {
      final options = DoctorsFilterCatalog.regionOptions([
        const DoctorModel(
          id: '1',
          firstName: 'A',
          lastName: 'B',
          fullName: 'A B',
          city: 'Samarqand',
        ),
      ]);
      expect(options, contains('Samarqand viloyati'));
    });
  });

  group('DoctorsFilterCatalog.applyCountryFilter', () {
    const uzDoctor = DoctorModel(
      id: '1',
      firstName: 'A',
      lastName: 'B',
      fullName: 'A B',
      city: 'Toshkent',
    );
    const deDoctor = DoctorModel(
      id: '2',
      firstName: 'C',
      lastName: 'D',
      fullName: 'C D',
      city: 'Berlin',
      locationCountry: 'Germany',
    );

    test('Germany excludes legacy Uzbek doctors without country', () {
      final filtered = DoctorsFilterCatalog.applyCountryFilter(
        const [uzDoctor, deDoctor],
        'Germany',
      );
      expect(filtered.map((d) => d.id), ['2']);
    });

    test('Uzbekistan includes legacy doctors without country', () {
      final filtered = DoctorsFilterCatalog.applyCountryFilter(
        const [uzDoctor, deDoctor],
        'Uzbekistan',
      );
      expect(filtered.map((d) => d.id), ['1']);
    });
  });

  group('UzbekistanRegions', () {
    test('displayLabel returns Uzbek for uz locale', () {
      expect(
        UzbekistanRegions.displayLabel('Samarqand viloyati', 'uz'),
        'Samarqand viloyati',
      );
    });

    test('canonicalFor resolves aliases', () {
      expect(UzbekistanRegions.canonicalFor('Tashkent'), 'Toshkent shahri');
      expect(UzbekistanRegions.canonicalFor('Samarqand'), 'Samarqand viloyati');
    });
  });
}
