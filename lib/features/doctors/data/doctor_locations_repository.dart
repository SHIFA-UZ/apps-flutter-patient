import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';

/// A single practice location for a doctor, as returned by
/// `GET /api/public/doctors/{id}/locations`. Patients select one of these
/// before choosing a time slot when the doctor has multiple locations.
class PublicDoctorLocation {
  final int id;
  final String label;
  final String? clinic;
  final String? address;
  final double? latitude;
  final double? longitude;
  final String? locationCountry;
  final String? locationRegion;
  final String? locationCity;
  final String? locationStreetAddress;
  final bool isPrimary;

  PublicDoctorLocation({
    required this.id,
    required this.label,
    this.clinic,
    this.address,
    this.latitude,
    this.longitude,
    this.locationCountry,
    this.locationRegion,
    this.locationCity,
    this.locationStreetAddress,
    this.isPrimary = false,
  });

  factory PublicDoctorLocation.fromJson(Map<String, dynamic> j) =>
      PublicDoctorLocation(
        id: (j['id'] as num).toInt(),
        label: (j['label'] as String?) ?? '',
        clinic: j['clinic'] as String?,
        address: j['address'] as String?,
        latitude: (j['latitude'] as num?)?.toDouble(),
        longitude: (j['longitude'] as num?)?.toDouble(),
        locationCountry: j['locationCountry'] as String?,
        locationRegion: j['locationRegion'] as String?,
        locationCity: j['locationCity'] as String?,
        locationStreetAddress: j['locationStreetAddress'] as String?,
        isPrimary: j['isPrimary'] == true,
      );

  /// Best-effort human-readable description of the location (clinic + city).
  String get displaySubtitle {
    final parts = <String>[];
    if (clinic != null && clinic!.isNotEmpty) parts.add(clinic!);
    if (address != null && address!.isNotEmpty) {
      parts.add(address!);
    } else {
      final cityParts = [
        locationCity,
        locationRegion,
        locationCountry,
      ].where((s) => s != null && s.isNotEmpty).cast<String>().toList();
      if (cityParts.isNotEmpty) parts.add(cityParts.join(', '));
    }
    return parts.join(' · ');
  }
}

class DoctorLocationsRepository {
  final ApiClient _apiClient;

  DoctorLocationsRepository(this._apiClient);

  /// GET /api/public/doctors/{id}/locations
  Future<List<PublicDoctorLocation>> getLocations(String doctorId) async {
    try {
      final response =
          await _apiClient.get('/public/doctors/$doctorId/locations');
      if (response.data is! List) return [];
      return (response.data as List)
          .map((e) => PublicDoctorLocation.fromJson(e as Map<String, dynamic>))
          .where((l) => l.id > 0) // ignore synthesized legacy placeholders
          .toList();
    } catch (_) {
      // Defensive: never block booking flow on a locations fetch failure.
      return const [];
    }
  }
}

final doctorLocationsRepositoryProvider =
    Provider<DoctorLocationsRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DoctorLocationsRepository(apiClient);
});
