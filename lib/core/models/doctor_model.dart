import 'package:equatable/equatable.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';

class DoctorModel extends Equatable {
  final String id;
  final String firstName;
  final String lastName;
  final String fullName;
  final String? profession;
  final String? clinic;
  final String? address;
  final String? street;
  final String? city;
  final String? region;
  final double? latitude;
  final double? longitude;
  final String? phone;
  final String? email;
  final String? photoUrl;
  final double? rating;
  final int? reviewCount;
  final List<String>? specializations;
  final String? furtherInformation;
  final String? biography;
  final List<String>? services;
  final List<String>? certificates;
  final String? telegram;
  final String? instagram;

  const DoctorModel({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.fullName,
    this.profession,
    this.clinic,
    this.address,
    this.street,
    this.city,
    this.region,
    this.latitude,
    this.longitude,
    this.phone,
    this.email,
    this.photoUrl,
    this.rating,
    this.reviewCount,
    this.specializations,
    this.furtherInformation,
    this.biography,
    this.services,
    this.certificates,
    this.telegram,
    this.instagram,
  });

  factory DoctorModel.fromJson(Map<String, dynamic> json) {
    return DoctorModel(
      id: json['id']?.toString() ?? '',
      firstName: json['firstName'] ?? '',
      lastName: json['lastName'] ?? '',
      fullName: json['fullName'] ?? '${json['firstName'] ?? ''} ${json['lastName'] ?? ''}'.trim(),
      profession: json['profession'] as String?,
      clinic: json['clinic'] as String?,
      address: json['address'] as String?,
      street: (json['locationStreetAddress'] ?? json['street']) as String?,
      city: (json['locationCity'] ?? json['city']) as String?,
      region: (json['locationRegion'] ?? json['region']) as String?,
      latitude: json['latitude'] != null ? (json['latitude'] as num).toDouble() : null,
      longitude: json['longitude'] != null ? (json['longitude'] as num).toDouble() : null,
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      photoUrl: normalizePhotoUrl(json['photoUrl'] as String?),
      rating: json['averageRating'] != null ? (json['averageRating'] as num).toDouble() : null,
      reviewCount: json['reviewCount'] != null ? (json['reviewCount'] as int) : null,
      specializations: json['specializations'] != null
          ? List<String>.from(json['specializations'])
          : null,
      furtherInformation: json['furtherInformation'] as String?,
      biography: json['biography'] as String?,
      services: json['services'] != null
          ? List<String>.from(json['services'])
          : null,
      certificates: json['certificates'] != null
          ? List<String>.from(json['certificates'])
          : null,
      telegram: json['telegram'] as String?,
      instagram: json['instagram'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'firstName': firstName,
      'lastName': lastName,
      'fullName': fullName,
      'profession': profession,
      'clinic': clinic,
      'address': address,
      'street': street,
      'city': city,
      'region': region,
      'latitude': latitude,
      'longitude': longitude,
      'phone': phone,
      'email': email,
      'photoUrl': photoUrl,
      'rating': rating,
      'reviewCount': reviewCount,
      'specializations': specializations,
      'furtherInformation': furtherInformation,
      'biography': biography,
      'services': services,
      'certificates': certificates,
      'telegram': telegram,
      'instagram': instagram,
    };
  }

  @override
  List<Object?> get props => [
        id,
        firstName,
        lastName,
        fullName,
        profession,
        clinic,
        address,
        street,
        city,
        region,
        latitude,
        longitude,
        phone,
        email,
        photoUrl,
        rating,
        reviewCount,
        specializations,
        furtherInformation,
        biography,
        services,
        certificates,
        telegram,
        instagram,
      ];
}
