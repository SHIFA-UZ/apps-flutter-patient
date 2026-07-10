import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';

/// Country matching for doctor discovery filters (handles legacy/incomplete location data).
class CountryCatalog {
  static const _germanyKeys = {'germany', 'deutschland', 'de'};
  static const _uzbekistanKeys = {
    'uzbekistan',
    "o'zbekiston",
    'ozbekiston',
    'uzbekistan republic',
    'uz',
  };

  static const _germanCities = {
    'berlin',
    'munich',
    'münchen',
    'hamburg',
    'cologne',
    'köln',
    'frankfurt',
    'stuttgart',
    'düsseldorf',
    'dortmund',
    'essen',
    'leipzig',
    'bremen',
    'dresden',
    'hannover',
    'nuremberg',
    'nürnberg',
  };

  static bool matches(DoctorModel doctor, String countryFilter) {
    final filter = countryFilter.trim().toLowerCase();
    if (filter.isEmpty) return true;

    if (_isGermanyFilter(filter)) return _doctorInGermany(doctor);
    if (_isUzbekistanFilter(filter)) return _doctorInUzbekistan(doctor);

    return _fieldMatches(doctor.locationCountry, {filter}) ||
        _fieldMatches(doctor.city, {filter}) ||
        _fieldMatches(doctor.region, {filter});
  }

  static bool _isGermanyFilter(String filter) =>
      filter == 'germany' || filter == 'deutschland' || filter == 'de';

  static bool _isUzbekistanFilter(String filter) =>
      _uzbekistanKeys.contains(filter) || filter.contains('uzbek');

  static bool _doctorInGermany(DoctorModel doctor) {
    if (_fieldMatches(doctor.locationCountry, _germanyKeys)) return true;
    if (_fieldMatches(doctor.city, _germanCities)) return true;
    if (_fieldMatches(doctor.region, {'berlin', 'bavaria', 'bayern'})) return true;
    return false;
  }

  static bool _doctorInUzbekistan(DoctorModel doctor) {
    if (_fieldMatches(doctor.locationCountry, _uzbekistanKeys)) return true;
    // Legacy doctors in the UZ app often have no country — treat unset country as Uzbekistan
    // unless they clearly look German/international.
    if (!_hasExplicitForeignCountry(doctor)) return true;
    return false;
  }

  static bool _hasExplicitForeignCountry(DoctorModel doctor) {
    if (_fieldMatches(doctor.locationCountry, _germanyKeys)) return true;
    if (_fieldMatches(doctor.city, _germanCities)) return true;
    final country = doctor.locationCountry?.trim().toLowerCase() ?? '';
    if (country.isEmpty) return false;
    return !_uzbekistanKeys.any((k) => country.contains(k));
  }

  static bool _fieldMatches(String? value, Set<String> keys) {
    if (value == null || value.trim().isEmpty) return false;
    final v = value.trim().toLowerCase();
    return keys.any((k) => v == k || v.contains(k));
  }
}
