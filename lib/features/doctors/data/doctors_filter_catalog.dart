import 'package:shifa_patient_app_v1/core/models/country_catalog.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/models/profession_model.dart';
import 'package:shifa_patient_app_v1/core/models/region_catalog.dart';

/// Builds filter option lists for the doctors discovery UI.
class DoctorsFilterCatalog {
  /// Region filter values (canonical where possible) merged from catalog + live doctor data.
  static List<String> regionOptions(
    List<DoctorModel> doctors, {
    String? countryFilter,
    String? languageCode,
  }) {
    final values = <String>{};

    final includeUzbekistanCatalog = countryFilter == null ||
        countryFilter.equals('Uzbekistan') ||
        countryFilter.equals("O'zbekiston") ||
        countryFilter.equals('UZ');

    if (includeUzbekistanCatalog) {
      values.addAll(UzbekistanRegions.canonicalValues);
    }

    for (final doctor in doctors) {
      _addRegionFromDoctor(values, doctor);
    }

    return UzbekistanRegions.sortKeysForDisplay(values, languageCode);
  }

  static void _addRegionFromDoctor(Set<String> values, DoctorModel doctor) {
    final region = doctor.region?.trim();
    if (region != null && region.isNotEmpty) {
      values.add(UzbekistanRegions.canonicalFor(region) ?? region);
    }

    final city = doctor.city?.trim();
    if (city != null && city.isNotEmpty) {
      final fromCity = UzbekistanRegions.canonicalFor(city);
      if (fromCity != null) {
        values.add(fromCity);
      } else if (region == null || region.isEmpty) {
        // International doctors: city often holds Berlin, Munich, etc.
        values.add(city);
      }
    }
  }

  static List<String> specialtyOptions(List<DoctorModel> doctors) {
    final set = doctors
        .map((d) => d.profession)
        .where((p) => p != null && p.trim().isNotEmpty)
        .map((p) => p!.trim())
        .toSet();
    return ProfessionData.sortEnglishKeysForDisplay(set, null);
  }

  /// Client-side country filter (works even when the API ignores the country param).
  static List<DoctorModel> applyCountryFilter(List<DoctorModel> doctors, String? country) {
    if (country == null || country.trim().isEmpty) return doctors;
    return doctors.where((d) => CountryCatalog.matches(d, country)).toList();
  }
}

extension _StringEquals on String {
  bool equals(String other) => toLowerCase() == other.toLowerCase();
}
