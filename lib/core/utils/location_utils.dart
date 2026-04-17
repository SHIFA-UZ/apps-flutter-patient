import 'dart:math' show cos, asin, sqrt;

/// Maximum radius (km) for "Doctors near me" on home. Only doctors within this distance are shown.
const double nearbyDoctorsMaxRadiusKm = 50.0;

/// Distance considered "close" for empty-state message when no doctors within this range.
const double nearbyDoctorsCloseRadiusKm = 15.0;

/// Distance between two points (Haversine formula).
/// Returns distance in kilometers, or null if either point is missing coordinates.
double? distanceKm(double? lat1, double? lon1, double? lat2, double? lon2) {
  if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
  const p = 0.017453292519943295; // pi / 180
  final a = 0.5 -
      cos((lat2 - lat1) * p) / 2 +
      cos(lat1 * p) * cos(lat2 * p) * (1 - cos((lon2 - lon1) * p)) / 2;
  return 12742 * asin(sqrt(a)); // 2 * R; R = 6371 km
}
