import 'package:equatable/equatable.dart';

/// Server-side filter parameters for the public doctors list API.
class DoctorsSearchFilters extends Equatable {
  final String? search;
  final String? profession;
  final String? clinic;
  final String? region;
  final String? country;
  final String? consultationType;
  final String? availableWithin;
  final double? minRating;
  final int? minPriceMinor;
  final int? maxPriceMinor;
  final bool? verifiedOnly;
  final double? latitude;
  final double? longitude;
  final double? radiusKm;
  final String? sortBy;
  final bool includeNextAvailable;
  final int page;
  final int pageSize;

  static const int defaultPageSize = 20;

  const DoctorsSearchFilters({
    this.search,
    this.profession,
    this.clinic,
    this.region,
    this.country,
    this.consultationType,
    this.availableWithin,
    this.minRating,
    this.minPriceMinor,
    this.maxPriceMinor,
    this.verifiedOnly,
    this.latitude,
    this.longitude,
    this.radiusKm,
    this.sortBy,
    this.includeNextAvailable = false,
    this.page = 1,
    this.pageSize = defaultPageSize,
  });

  DoctorsSearchFilters copyWith({
    String? search,
    String? profession,
    String? clinic,
    String? region,
    String? country,
    String? consultationType,
    String? availableWithin,
    double? minRating,
    int? minPriceMinor,
    int? maxPriceMinor,
    bool? verifiedOnly,
    double? latitude,
    double? longitude,
    double? radiusKm,
    String? sortBy,
    bool? includeNextAvailable,
    int? page,
    int? pageSize,
    bool clearSearch = false,
    bool clearProfession = false,
    bool clearClinic = false,
    bool clearRegion = false,
    bool clearCountry = false,
    bool clearConsultationType = false,
    bool clearAvailableWithin = false,
    bool clearMinRating = false,
    bool clearMinPriceMinor = false,
    bool clearMaxPriceMinor = false,
    bool clearVerifiedOnly = false,
    bool clearLatitude = false,
    bool clearLongitude = false,
    bool clearRadiusKm = false,
    bool clearSortBy = false,
  }) {
    return DoctorsSearchFilters(
      search: clearSearch ? null : (search ?? this.search),
      profession: clearProfession ? null : (profession ?? this.profession),
      clinic: clearClinic ? null : (clinic ?? this.clinic),
      region: clearRegion ? null : (region ?? this.region),
      country: clearCountry ? null : (country ?? this.country),
      consultationType:
          clearConsultationType ? null : (consultationType ?? this.consultationType),
      availableWithin:
          clearAvailableWithin ? null : (availableWithin ?? this.availableWithin),
      minRating: clearMinRating ? null : (minRating ?? this.minRating),
      minPriceMinor: clearMinPriceMinor ? null : (minPriceMinor ?? this.minPriceMinor),
      maxPriceMinor: clearMaxPriceMinor ? null : (maxPriceMinor ?? this.maxPriceMinor),
      verifiedOnly: clearVerifiedOnly ? null : (verifiedOnly ?? this.verifiedOnly),
      latitude: clearLatitude ? null : (latitude ?? this.latitude),
      longitude: clearLongitude ? null : (longitude ?? this.longitude),
      radiusKm: clearRadiusKm ? null : (radiusKm ?? this.radiusKm),
      sortBy: clearSortBy ? null : (sortBy ?? this.sortBy),
      includeNextAvailable: includeNextAvailable ?? this.includeNextAvailable,
      page: page ?? this.page,
      pageSize: pageSize ?? this.pageSize,
    );
  }

  Map<String, dynamic> toQueryParameters() {
    return {
      if (search != null && search!.isNotEmpty) 'search': search,
      if (profession != null && profession!.isNotEmpty) 'profession': profession,
      if (clinic != null && clinic!.isNotEmpty) 'clinic': clinic,
      if (region != null && region!.isNotEmpty) 'region': region,
      if (country != null && country!.isNotEmpty) 'country': country,
      if (consultationType != null && consultationType!.isNotEmpty)
        'consultationType': consultationType,
      if (availableWithin != null && availableWithin!.isNotEmpty)
        'availableWithin': availableWithin,
      if (minRating != null) 'minRating': minRating,
      if (minPriceMinor != null) 'minPriceMinor': minPriceMinor,
      if (maxPriceMinor != null) 'maxPriceMinor': maxPriceMinor,
      if (verifiedOnly == true) 'verifiedOnly': true,
      if (latitude != null) 'latitude': latitude,
      if (longitude != null) 'longitude': longitude,
      if (radiusKm != null) 'radiusKm': radiusKm,
      if (sortBy != null && sortBy!.isNotEmpty) 'sortBy': sortBy,
      if (includeNextAvailable) 'includeNextAvailable': true,
      if (page > 1) 'page': page,
      'pageSize': pageSize,
    };
  }

  @override
  List<Object?> get props => [
        search,
        profession,
        clinic,
        region,
        country,
        consultationType,
        availableWithin,
        minRating,
        minPriceMinor,
        maxPriceMinor,
        verifiedOnly,
        latitude,
        longitude,
        radiusKm,
        sortBy,
        includeNextAvailable,
        page,
        pageSize,
      ];
}
