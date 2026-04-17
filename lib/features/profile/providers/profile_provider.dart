import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:shifa_patient_app_v1/core/models/patient_profile_model.dart';
import 'package:shifa_patient_app_v1/features/profile/data/profile_repository.dart';

class ProfileState {
  final PatientProfileModel? profile;
  final bool isLoading;
  final String? error;
  final int photoCacheKey;

  ProfileState({
    this.profile,
    this.isLoading = false,
    this.error,
    this.photoCacheKey = 0,
  });

  ProfileState copyWith({
    PatientProfileModel? profile,
    bool? isLoading,
    String? error,
    int? photoCacheKey,
  }) {
    return ProfileState(
      profile: profile ?? this.profile,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      photoCacheKey: photoCacheKey ?? this.photoCacheKey,
    );
  }
}

class ProfileNotifier extends StateNotifier<ProfileState> {
  final ProfileRepository _repository;

  ProfileNotifier(this._repository) : super(ProfileState());

  Future<void> loadProfile() async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final profile = await _repository.getProfile();
      final cacheKey = profile.photoUrl != state.profile?.photoUrl
          ? DateTime.now().millisecondsSinceEpoch
          : state.photoCacheKey;
      state = state.copyWith(
        profile: profile,
        isLoading: false,
        photoCacheKey: cacheKey,
      );
    } catch (e) {
      // 401: token invalid - don't set error state; ApiClient interceptor triggers logout
      if (e is DioException && e.response?.statusCode == 401) {
        state = state.copyWith(isLoading: false);
        return;
      }
      state = state.copyWith(
        error: e.toString(),
        isLoading: false,
      );
    }
  }

  Future<void> updateProfile({
    String? firstName,
    String? lastName,
    String? birthDate,
    String? gender,
    String? phone,
    String? address,
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
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final updatedProfile = await _repository.updateProfile(
        firstName: firstName,
        lastName: lastName,
        birthDate: birthDate,
        gender: gender,
        phone: phone,
        address: address,
        language: language,
        photoUrl: photoUrl,
        latitude: latitude,
        longitude: longitude,
        locationCountry: locationCountry,
        locationRegion: locationRegion,
        locationDistrict: locationDistrict,
        locationCity: locationCity,
        locationPostalCode: locationPostalCode,
        locationStreetAddress: locationStreetAddress,
        timeZone: timeZone,
      );
      final cacheKey = photoUrl != null
          ? DateTime.now().millisecondsSinceEpoch
          : state.photoCacheKey;
      state = state.copyWith(
        profile: updatedProfile,
        isLoading: false,
        photoCacheKey: cacheKey,
      );
    } catch (e) {
      // 401: token invalid - don't set error state; ApiClient interceptor triggers logout
      if (e is DioException && e.response?.statusCode == 401) {
        state = state.copyWith(isLoading: false);
        return;
      }
      state = state.copyWith(
        error: e.toString(),
        isLoading: false,
      );
      rethrow;
    }
  }

  /// Syncs device timezone to backend silently (no UI, no loading). Called on app
  /// start when authenticated and on app resume so travel/timezone changes are picked up.
  Future<void> syncTimeZoneFromDevice() async {
    try {
      final deviceTz = await FlutterTimezone.getLocalTimezone();
      if (deviceTz.isEmpty) return;
      if (state.profile?.timeZone == deviceTz) return;
      final updated = await _repository.updateProfile(timeZone: deviceTz);
      state = state.copyWith(profile: updated);
      AppLogger.debug('[Profile] timezone synced to backend: $deviceTz');
    } catch (e) {
      AppLogger.debug('[Profile] timezone sync failed (ignored): $e');
    }
  }
}

final profileProvider = StateNotifierProvider<ProfileNotifier, ProfileState>((ref) {
  final repository = ref.watch(profileRepositoryProvider);
  return ProfileNotifier(repository);
});
