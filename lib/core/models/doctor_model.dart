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
  final List<DoctorServiceItem>? serviceItems;
  final List<String>? certificates;
  final String? telegram;
  final String? instagram;
  final String? nextAvailableStartAt;
  final String? recommendationReason;
  final List<String>? triggeredBySymptoms;
  final String? locationCountry;
  final double? distanceKm;
  final bool supportsOnline;
  final bool supportsInPerson;
  final int? minPriceMinor;
  final String? minPriceCurrency;
  final int? onlineMinPriceMinor;
  final int? clinicMinPriceMinor;

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
    this.serviceItems,
    this.certificates,
    this.telegram,
    this.instagram,
    this.nextAvailableStartAt,
    this.recommendationReason,
    this.triggeredBySymptoms,
    this.locationCountry,
    this.distanceKm,
    this.supportsOnline = false,
    this.supportsInPerson = false,
    this.minPriceMinor,
    this.minPriceCurrency,
    this.onlineMinPriceMinor,
    this.clinicMinPriceMinor,
  });

  DoctorModel copyWith({
    String? nextAvailableStartAt,
    bool? supportsOnline,
    bool? supportsInPerson,
    int? minPriceMinor,
    String? minPriceCurrency,
    int? onlineMinPriceMinor,
    int? clinicMinPriceMinor,
    String? locationCountry,
    double? distanceKm,
  }) {
    return DoctorModel(
      id: id,
      firstName: firstName,
      lastName: lastName,
      fullName: fullName,
      profession: profession,
      clinic: clinic,
      address: address,
      street: street,
      city: city,
      region: region,
      latitude: latitude,
      longitude: longitude,
      phone: phone,
      email: email,
      photoUrl: photoUrl,
      rating: rating,
      reviewCount: reviewCount,
      specializations: specializations,
      furtherInformation: furtherInformation,
      biography: biography,
      services: services,
      serviceItems: serviceItems,
      certificates: certificates,
      telegram: telegram,
      instagram: instagram,
      nextAvailableStartAt: nextAvailableStartAt ?? this.nextAvailableStartAt,
      recommendationReason: recommendationReason,
      triggeredBySymptoms: triggeredBySymptoms,
      locationCountry: locationCountry ?? this.locationCountry,
      distanceKm: distanceKm ?? this.distanceKm,
      supportsOnline: supportsOnline ?? this.supportsOnline,
      supportsInPerson: supportsInPerson ?? this.supportsInPerson,
      minPriceMinor: minPriceMinor ?? this.minPriceMinor,
      minPriceCurrency: minPriceCurrency ?? this.minPriceCurrency,
      onlineMinPriceMinor: onlineMinPriceMinor ?? this.onlineMinPriceMinor,
      clinicMinPriceMinor: clinicMinPriceMinor ?? this.clinicMinPriceMinor,
    );
  }

  /// Online video consult when API flag missing but services exist.
  bool get effectiveSupportsOnline =>
      supportsOnline || (serviceItems != null && serviceItems!.isNotEmpty);

  /// In-person when API flag missing but clinic/location exists.
  bool get effectiveSupportsInPerson =>
      supportsInPerson ||
      (clinic != null && clinic!.trim().isNotEmpty) ||
      (city != null && city!.trim().isNotEmpty) ||
      latitude != null;

  /// Cheapest display price from list API or service items.
  (int amountMinor, String currency)? get startingPrice {
    if (minPriceMinor != null &&
        minPriceCurrency != null &&
        minPriceCurrency!.isNotEmpty) {
      return (minPriceMinor!, minPriceCurrency!);
    }
    int? best;
    String? currency;
    for (final item in serviceItems ?? const <DoctorServiceItem>[]) {
      if (item.isFreeConsultation) continue;
      for (final price in item.prices) {
        if (best == null || price.amountMinor < best) {
          best = price.amountMinor;
          currency = price.currency;
        }
      }
    }
    if (best == null || currency == null) return null;
    return (best, currency);
  }

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
      reviewCount: json['reviewCount'] != null ? (json['reviewCount'] as num).toInt() : null,
      specializations: json['specializations'] != null
          ? List<String>.from(json['specializations'])
          : null,
      furtherInformation: json['furtherInformation'] as String?,
      biography: json['biography'] as String?,
      services: json['services'] != null
          ? List<String>.from(json['services'])
          : null,
      serviceItems: json['serviceItems'] != null
          ? (json['serviceItems'] as List)
              .map((e) => DoctorServiceItem.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
      certificates: json['certificates'] != null
          ? List<String>.from(json['certificates'])
          : null,
      telegram: json['telegram'] as String?,
      instagram: json['instagram'] as String?,
      nextAvailableStartAt: json['nextAvailableStartAt'] as String?,
      recommendationReason: json['recommendationReason'] as String?,
      triggeredBySymptoms: json['triggeredBySymptoms'] != null
          ? List<String>.from(json['triggeredBySymptoms'])
          : null,
      locationCountry: json['locationCountry'] as String?,
      distanceKm: json['distanceKm'] != null ? (json['distanceKm'] as num).toDouble() : null,
      supportsOnline: json['supportsOnline'] == true,
      supportsInPerson: json['supportsInPerson'] == true,
      minPriceMinor: json['minPriceMinor'] != null ? (json['minPriceMinor'] as num).toInt() : null,
      minPriceCurrency: json['minPriceCurrency'] as String?,
      onlineMinPriceMinor:
          json['onlineMinPriceMinor'] != null ? (json['onlineMinPriceMinor'] as num).toInt() : null,
      clinicMinPriceMinor:
          json['clinicMinPriceMinor'] != null ? (json['clinicMinPriceMinor'] as num).toInt() : null,
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
      'serviceItems': serviceItems?.map((e) => e.toJson()).toList(),
      'certificates': certificates,
      'telegram': telegram,
      'instagram': instagram,
      'nextAvailableStartAt': nextAvailableStartAt,
      'recommendationReason': recommendationReason,
      'triggeredBySymptoms': triggeredBySymptoms,
      'locationCountry': locationCountry,
      'distanceKm': distanceKm,
      'supportsOnline': supportsOnline,
      'supportsInPerson': supportsInPerson,
      'minPriceMinor': minPriceMinor,
      'minPriceCurrency': minPriceCurrency,
      'onlineMinPriceMinor': onlineMinPriceMinor,
      'clinicMinPriceMinor': clinicMinPriceMinor,
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
        serviceItems,
        certificates,
        telegram,
        instagram,
        nextAvailableStartAt,
        recommendationReason,
        triggeredBySymptoms,
        locationCountry,
        distanceKm,
        supportsOnline,
        supportsInPerson,
        minPriceMinor,
        minPriceCurrency,
        onlineMinPriceMinor,
        clinicMinPriceMinor,
      ];
}

class DoctorServiceItem extends Equatable {
  final String id;
  final String title;
  final String? description;
  /// When true, video bookings skip payment for this service.
  final bool isFreeConsultation;
  final List<DoctorServicePriceItem> prices;

  const DoctorServiceItem({
    required this.id,
    required this.title,
    this.description,
    this.isFreeConsultation = false,
    this.prices = const [],
  });

  factory DoctorServiceItem.fromJson(Map<String, dynamic> json) {
    return DoctorServiceItem(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      description: json['description'] as String?,
      isFreeConsultation: json['isFreeConsultation'] == true,
      prices: (json['prices'] as List? ?? const [])
          .map((e) => DoctorServicePriceItem.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'description': description,
        'isFreeConsultation': isFreeConsultation,
        'prices': prices.map((e) => e.toJson()).toList(),
      };

  @override
  List<Object?> get props => [id, title, description, isFreeConsultation, prices];
}

class DoctorServicePriceItem extends Equatable {
  final int amountMinor;
  final String currency;

  const DoctorServicePriceItem({
    required this.amountMinor,
    required this.currency,
  });

  factory DoctorServicePriceItem.fromJson(Map<String, dynamic> json) {
    return DoctorServicePriceItem(
      amountMinor: (json['amountMinor'] as num?)?.toInt() ?? 0,
      currency: json['currency']?.toString() ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
        'amountMinor': amountMinor,
        'currency': currency,
      };

  @override
  List<Object?> get props => [amountMinor, currency];
}
