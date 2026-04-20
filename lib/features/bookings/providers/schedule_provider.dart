import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/schedule_repository.dart';

class ScheduleState {
  final List<AvailableSlot> availableSlots;
  final bool isLoading;
  final String? error;

  ScheduleState({
    this.availableSlots = const [],
    this.isLoading = false,
    this.error,
  });

  ScheduleState copyWith({
    List<AvailableSlot>? availableSlots,
    bool? isLoading,
    String? error,
  }) {
    return ScheduleState(
      availableSlots: availableSlots ?? this.availableSlots,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}

class ScheduleNotifier extends StateNotifier<ScheduleState> {
  final ScheduleRepository _repository;

  ScheduleNotifier(this._repository) : super(ScheduleState());

  /// Loads available slots for a day and returns the list (for finding next available date).
  /// When [locationId] is provided, filters to that practice location.
  Future<List<AvailableSlot>> loadAvailableSlots({
    required String doctorId,
    required String day, // yyyy-MM-dd
    int? locationId,
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final slots = await _repository.getAvailableSlots(
        doctorId: doctorId,
        day: day,
        locationId: locationId,
      );
      state = state.copyWith(availableSlots: slots, isLoading: false);
      return slots;
    } catch (e) {
      state = state.copyWith(
        error: e.toString(),
        isLoading: false,
      );
      return [];
    }
  }

  void clearSlots() {
    state = ScheduleState();
  }
}

final scheduleProvider = StateNotifierProvider<ScheduleNotifier, ScheduleState>((ref) {
  final repository = ref.watch(scheduleRepositoryProvider);
  return ScheduleNotifier(repository);
});
