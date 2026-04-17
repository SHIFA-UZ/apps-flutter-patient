import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';
import 'package:shifa_patient_app_v1/features/doctors/data/doctors_repository.dart';

class DoctorsState {
  /// All doctors (for "Recommended" tab)
  final List<DoctorModel> doctors;
  /// Doctors the patient has ever had an appointment with (for "My Doctors" tab)
  final List<DoctorModel> myDoctors;
  final bool isLoading;
  final bool isLoadingMyDoctors;
  final String? error;
  final String? errorMyDoctors;
  final String? searchQuery;

  DoctorsState({
    this.doctors = const [],
    this.myDoctors = const [],
    this.isLoading = false,
    this.isLoadingMyDoctors = false,
    this.error,
    this.errorMyDoctors,
    this.searchQuery,
  });

  DoctorsState copyWith({
    List<DoctorModel>? doctors,
    List<DoctorModel>? myDoctors,
    bool? isLoading,
    bool? isLoadingMyDoctors,
    String? error,
    String? errorMyDoctors,
    String? searchQuery,
  }) {
    return DoctorsState(
      doctors: doctors ?? this.doctors,
      myDoctors: myDoctors ?? this.myDoctors,
      isLoading: isLoading ?? this.isLoading,
      isLoadingMyDoctors: isLoadingMyDoctors ?? this.isLoadingMyDoctors,
      error: error,
      errorMyDoctors: errorMyDoctors,
      searchQuery: searchQuery ?? this.searchQuery,
    );
  }
}

class DoctorsNotifier extends StateNotifier<DoctorsState> {
  final Ref _ref;
  final DoctorsRepository _repository;

  DoctorsNotifier(this._ref, this._repository) : super(DoctorsState());

  /// Load all doctors (for Recommended tab)
  Future<void> searchDoctors({
    String? search,
    String? profession,
    String? clinic,
  }) async {
    state = state.copyWith(isLoading: true, error: null, searchQuery: search);
    try {
      final doctors = await _repository.searchDoctors(
        search: search,
        profession: profession,
        clinic: clinic,
      );
      state = state.copyWith(doctors: doctors, isLoading: false);
    } catch (e) {
      state = state.copyWith(
        error: e.toString(),
        isLoading: false,
      );
    }
  }

  Future<void> loadDoctors() async {
    await searchDoctors();
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
