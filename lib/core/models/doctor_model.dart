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
      ];
}

class DoctorServiceItem extends Equatable {
  final String id;
  final String title;
  final String? description;
  /// When true, video bookings skip payment for this service.
  final bool isFreeConsultation;
  final List<DoctorServicePriceItem> prices;
  final String? groupId;
  final String? groupName;
  final int? groupSortOrder;

  const DoctorServiceItem({
    required this.id,
    required this.title,
    this.description,
    this.isFreeConsultation = false,
    this.prices = const [],
    this.groupId,
    this.groupName,
    this.groupSortOrder,
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
      groupId: json['groupId']?.toString(),
      groupName: json['groupName'] as String?,
      groupSortOrder: (json['groupSortOrder'] as num?)?.toInt(),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'description': description,
        'isFreeConsultation': isFreeConsultation,
        'prices': prices.map((e) => e.toJson()).toList(),
        if (groupId != null) 'groupId': groupId,
        if (groupName != null) 'groupName': groupName,
        if (groupSortOrder != null) 'groupSortOrder': groupSortOrder,
      };

  /// Card teaser: prefers default (all-locations) price; falls back to any configured row.
  String teaserPriceLabel({required String priceNotSetLabel}) {
    if (isFreeConsultation) return '';
    final globals = prices.where((p) => p.locationId == null).toList();
    final lineSrc = globals.isNotEmpty ? globals : prices;
    if (lineSrc.isEmpty) return priceNotSetLabel;
    final sorted = [...lineSrc]..sort((a, b) => a.currency.compareTo(b.currency));
    final p = sorted.first;
    return '${(p.amountMinor / 100).toStringAsFixed(2)} ${p.currency}';
  }

  @override
  List<Object?> get props => [
        id,
        title,
        description,
        isFreeConsultation,
        prices,
        groupId,
        groupName,
        groupSortOrder,
      ];
}

class DoctorServicePriceItem extends Equatable {
  final int amountMinor;
  final String currency;
  /// When null, this row is the default for all locations (unless a location-specific row exists).
  final int? locationId;

  const DoctorServicePriceItem({
    required this.amountMinor,
    required this.currency,
    this.locationId,
  });

  factory DoctorServicePriceItem.fromJson(Map<String, dynamic> json) {
    return DoctorServicePriceItem(
      amountMinor: (json['amountMinor'] as num?)?.toInt() ?? 0,
      currency: json['currency']?.toString() ?? '',
      locationId: (json['locationId'] as num?)?.toInt(),
    );
  }

  Map<String, dynamic> toJson() => {
        'amountMinor': amountMinor,
        'currency': currency,
        if (locationId != null) 'locationId': locationId,
      };

  @override
  List<Object?> get props => [amountMinor, currency, locationId];
}
