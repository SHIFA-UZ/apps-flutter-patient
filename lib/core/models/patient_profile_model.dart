import 'package:equatable/equatable.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';

class PatientProfileModel extends Equatable {
  final String? id;
  final String fullName;
  final String? phone;
  final String? email;
  final String? address; // Legacy field for backward compatibility
  final String? birthDate;
  final String? language;
  final String? photoUrl;
  final double? latitude;
  final double? longitude;
  
  // Structured location fields
  final String? locationCountry;
  final String? locationRegion;
  final String? locationDistrict;
  final String? locationCity;
  final String? locationPostalCode;
  final String? locationStreetAddress;
  final String? timeZone;

  /// Admin-managed subscription tier ("PRO" or "PREMIUM" for patients).
  /// Drives feature gating in the patient app (e.g. Shifa AI).
  final String? subscriptionTier;

  /// Features granted by the current tier (mirrors `SubscriptionFeature` names).
  final List<String> features;

  const PatientProfileModel({
    this.id,
    required this.fullName,
    this.phone,
    this.email,
    this.address,
    this.birthDate,
    this.language,
    this.photoUrl,
    this.latitude,
    this.longitude,
    this.locationCountry,
    this.locationRegion,
    this.locationDistrict,
    this.locationCity,
    this.locationPostalCode,
    this.locationStreetAddress,
    this.timeZone,
    this.subscriptionTier,
    this.features = const [],
  });

  factory PatientProfileModel.fromJson(Map<String, dynamic> json) {
    return PatientProfileModel(
      id: json['id']?.toString(),
      fullName: json['fullName'] ?? '',
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      address: json['address'] as String?,
      birthDate: json['birthDate'] as String?,
      language: json['language'] as String?,
      photoUrl: normalizePhotoUrl(json['photoUrl'] as String?),
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      locationCountry: json['locationCountry'] as String?,
      locationRegion: json['locationRegion'] as String?,
      locationDistrict: json['locationDistrict'] as String?,
      locationCity: json['locationCity'] as String?,
      locationPostalCode: json['locationPostalCode'] as String?,
      locationStreetAddress: json['locationStreetAddress'] as String?,
      timeZone: json['timeZone'] as String?,
      subscriptionTier: json['subscriptionTier'] as String?,
      features: (json['features'] as List<dynamic>?)
              ?.map((e) => e.toString())
              .toList() ??
          const [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'fullName': fullName,
      'phone': phone,
      'email': email,
      'address': address,
      'birthDate': birthDate,
      'language': language,
      'photoUrl': photoUrl,
      'latitude': latitude,
      'longitude': longitude,
      'locationCountry': locationCountry,
      'locationRegion': locationRegion,
      'locationDistrict': locationDistrict,
      'locationCity': locationCity,
      'locationPostalCode': locationPostalCode,
      'locationStreetAddress': locationStreetAddress,
      'timeZone': timeZone,
      'subscriptionTier': subscriptionTier,
      'features': features,
    };
  }

  PatientProfileModel copyWith({
    String? id,
    String? fullName,
    String? phone,
    String? email,
    String? address,
    String? birthDate,
    String? language,
    String? photoUrl,
    double? latitude,
    double? longitude,
    String? locationCountry,
    String? locationRegion,
    String? locationDistrict,
    String? locationCity,
    String? locationPostalCode,
    String? locationStreetAddress,
    String? timeZone,
    String? subscriptionTier,
    List<String>? features,
  }) {
    return PatientProfileModel(
      id: id ?? this.id,
      fullName: fullName ?? this.fullName,
      phone: phone ?? this.phone,
      email: email ?? this.email,
      address: address ?? this.address,
      birthDate: birthDate ?? this.birthDate,
      language: language ?? this.language,
      photoUrl: photoUrl ?? this.photoUrl,
      latitude: latitude ?? this.latitude,
      longitude: longitude ?? this.longitude,
      locationCountry: locationCountry ?? this.locationCountry,
      locationRegion: locationRegion ?? this.locationRegion,
      locationDistrict: locationDistrict ?? this.locationDistrict,
      locationCity: locationCity ?? this.locationCity,
      locationPostalCode: locationPostalCode ?? this.locationPostalCode,
      locationStreetAddress: locationStreetAddress ?? this.locationStreetAddress,
      timeZone: timeZone ?? this.timeZone,
      subscriptionTier: subscriptionTier ?? this.subscriptionTier,
      features: features ?? this.features,
    );
  }

  @override
  List<Object?> get props => [
        id,
        fullName,
        phone,
        email,
        address,
        birthDate,
        language,
        photoUrl,
        latitude,
        longitude,
        locationCountry,
        locationRegion,
        locationDistrict,
        locationCity,
        locationPostalCode,
        locationStreetAddress,
        timeZone,
        subscriptionTier,
        features,
      ];
}
