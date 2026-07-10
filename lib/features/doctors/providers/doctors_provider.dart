import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';

import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';

import 'package:shifa_patient_app_v1/features/bookings/data/schedule_repository.dart';

import 'package:shifa_patient_app_v1/features/doctors/data/doctor_availability_enricher.dart';

import 'package:shifa_patient_app_v1/features/doctors/data/doctors_search_filters.dart';

import 'package:shifa_patient_app_v1/features/doctors/data/doctors_filter_catalog.dart';

import 'package:shifa_patient_app_v1/features/doctors/data/doctors_repository.dart';

import 'package:shifa_patient_app_v1/core/models/region_catalog.dart';



class DoctorsState {

  /// Doctors for the current Recommended-tab page.

  final List<DoctorModel> doctors;

  /// Doctors the patient has ever had an appointment with (for "Recent" tab)

  final List<DoctorModel> myDoctors;

  final int totalCount;

  final int currentPage;

  final int pageSize;

  final DoctorsSearchFilters activeFilters;

  final List<String> catalogRegionOptions;

  final List<String> catalogSpecialtyOptions;

  final bool isLoading;

  final bool isLoadingMyDoctors;

  final bool isEnrichingAvailability;

  final String? error;

  final String? errorMyDoctors;

  final String? searchQuery;



  const DoctorsState({

    this.doctors = const [],

    this.myDoctors = const [],

    this.totalCount = 0,

    this.currentPage = 1,

    this.pageSize = DoctorsSearchFilters.defaultPageSize,

    this.activeFilters = const DoctorsSearchFilters(),

    this.catalogRegionOptions = const [],

    this.catalogSpecialtyOptions = const [],

    this.isLoading = false,

    this.isLoadingMyDoctors = false,

    this.isEnrichingAvailability = false,

    this.error,

    this.errorMyDoctors,

    this.searchQuery,

  });



  int get totalPages {

    if (totalCount <= 0) return 1;

    return (totalCount / pageSize).ceil();

  }



  DoctorsState copyWith({

    List<DoctorModel>? doctors,

    List<DoctorModel>? myDoctors,

    int? totalCount,

    int? currentPage,

    int? pageSize,

    DoctorsSearchFilters? activeFilters,

    List<String>? catalogRegionOptions,

    List<String>? catalogSpecialtyOptions,

    bool? isLoading,

    bool? isLoadingMyDoctors,

    bool? isEnrichingAvailability,

    String? error,

    String? errorMyDoctors,

    String? searchQuery,

  }) {

    return DoctorsState(

      doctors: doctors ?? this.doctors,

      myDoctors: myDoctors ?? this.myDoctors,

      totalCount: totalCount ?? this.totalCount,

      currentPage: currentPage ?? this.currentPage,

      pageSize: pageSize ?? this.pageSize,

      activeFilters: activeFilters ?? this.activeFilters,

      catalogRegionOptions: catalogRegionOptions ?? this.catalogRegionOptions,

      catalogSpecialtyOptions: catalogSpecialtyOptions ?? this.catalogSpecialtyOptions,

      isLoading: isLoading ?? this.isLoading,

      isLoadingMyDoctors: isLoadingMyDoctors ?? this.isLoadingMyDoctors,

      isEnrichingAvailability: isEnrichingAvailability ?? this.isEnrichingAvailability,

      error: error,

      errorMyDoctors: errorMyDoctors,

      searchQuery: searchQuery ?? this.searchQuery,

    );

  }

}



class DoctorsNotifier extends StateNotifier<DoctorsState> {

  final Ref _ref;

  final DoctorsRepository _repository;

  int _fetchGeneration = 0;



  DoctorsNotifier(this._ref, this._repository) : super(const DoctorsState());



  Future<void> loadFilterCatalog() async {

    try {

      final result = await _repository.searchDoctors(const DoctorsSearchFilters());

      state = state.copyWith(

        catalogRegionOptions: DoctorsFilterCatalog.regionOptions(result.doctors),

        catalogSpecialtyOptions: DoctorsFilterCatalog.specialtyOptions(result.doctors),

      );

    } catch (_) {

      state = state.copyWith(

        catalogRegionOptions: UzbekistanRegions.canonicalValues,

      );

    }

  }



  /// Load doctors for Recommended tab with server-side filters.

  Future<void> searchDoctors({DoctorsSearchFilters? filters}) async {

    final nextFilters = filters ?? state.activeFilters;

    await _fetchDoctors(nextFilters, updateActiveFilters: true);

  }



  /// Loads the full unfiltered doctor list for home, create booking, etc.

  Future<void> loadDoctors() async {

    await _fetchDoctors(const DoctorsSearchFilters(), updateActiveFilters: false);

  }



  Future<void> _fetchDoctors(

    DoctorsSearchFilters filters, {

    required bool updateActiveFilters,

  }) async {

    final generation = ++_fetchGeneration;

    state = state.copyWith(

      isLoading: true,

      isEnrichingAvailability: false,

      error: null,

      searchQuery: filters.search,

      currentPage: filters.page,

      pageSize: filters.pageSize,

      activeFilters: updateActiveFilters ? filters : state.activeFilters,

    );

    try {

      final result = await _repository.searchDoctors(filters);

      final serverPaginated = result.page != null;



      var doctors = DoctorsFilterCatalog.applyCountryFilter(

        result.doctors,

        filters.country,

      );

      var totalCount = result.totalCount;



      if (!serverPaginated) {

        totalCount = doctors.length;

        final start = (filters.page - 1) * filters.pageSize;

        doctors = doctors.skip(start).take(filters.pageSize).toList();

      }



      state = state.copyWith(

        doctors: doctors,

        totalCount: totalCount,

        isLoading: false,

      );



      if (updateActiveFilters &&

          filters.includeNextAvailable &&

          !serverPaginated) {

        _enrichAvailabilityInBackground(generation, doctors);

      }

    } catch (e) {

      state = state.copyWith(

        error: e.toString(),

        isLoading: false,

      );

    }

  }



  Future<void> _enrichAvailabilityInBackground(

    int generation,

    List<DoctorModel> doctors,

  ) async {

    state = state.copyWith(isEnrichingAvailability: true);

    final enricher = DoctorAvailabilityEnricher(

      _ref.read(scheduleRepositoryProvider),

    );

    final enriched = await enricher.enrichMissingAvailability(

      doctors,

      maxDoctors: doctors.length,

    );

    if (generation != _fetchGeneration) return;

    state = state.copyWith(

      doctors: enriched,

      isEnrichingAvailability: false,

    );

  }



  /// Load doctors the patient has ever had an appointment with (for My Doctors tab)

  Future<void> loadMyDoctors() async {

    state = state.copyWith(isLoadingMyDoctors: true, errorMyDoctors: null);

    try {

      final bookingsRepo = _ref.read(bookingsRepositoryProvider);

      final appointments = await bookingsRepo.getAppointments();

      final doctorIds = appointments

          .map((a) => a.doctorId)

          .where((id) => id.isNotEmpty)

          .toSet()

          .toList();



      if (doctorIds.isEmpty) {

        state = state.copyWith(myDoctors: [], isLoadingMyDoctors: false);

        return;

      }



      final List<DoctorModel> myDoctorsList = [];

      for (final id in doctorIds) {

        try {

          final doctor = await _repository.getDoctorById(id);

          myDoctorsList.add(doctor);

        } catch (_) {

          // Skip if one doctor fails to load (e.g. deleted)

        }

      }

      state = state.copyWith(myDoctors: myDoctorsList, isLoadingMyDoctors: false);

    } catch (e) {

      state = state.copyWith(

        errorMyDoctors: e.toString(),

        isLoadingMyDoctors: false,

      );

    }

  }



  Future<DoctorModel> getDoctorById(String doctorId) async {

    try {

      return await _repository.getDoctorById(doctorId);

    } catch (e) {

      throw Exception('Failed to load doctor: $e');

    }

  }

}



final doctorsProvider = StateNotifierProvider<DoctorsNotifier, DoctorsState>((ref) {

  final repository = ref.watch(doctorsRepositoryProvider);

  return DoctorsNotifier(ref, repository);

});


