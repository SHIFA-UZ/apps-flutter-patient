import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/appointment_model.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';

class BookingsState {
  final List<AppointmentModel> appointments;
  final bool isLoading;
  final String? error;

  BookingsState({
    this.appointments = const [],
    this.isLoading = false,
    this.error,
  });

  BookingsState copyWith({
    List<AppointmentModel>? appointments,
    bool? isLoading,
    String? error,
  }) {
    return BookingsState(
      appointments: appointments ?? this.appointments,
      isLoading: isLoading ?? this.isLoading,
      error: error,
    );
  }
}

class BookingsNotifier extends StateNotifier<BookingsState> {
  final BookingsRepository _repository;

  BookingsNotifier(this._repository) : super(BookingsState());

  Future<void> loadAppointments({
    String? status,
    String? startDate,
    String? endDate,
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    try {
      final appointments = await _repository.getAppointments(
        status: status,
        startDate: startDate,
        endDate: endDate,
      );
      state = state.copyWith(appointments: appointments, isLoading: false);
    } catch (e) {
      state = state.copyWith(
        error: e.toString(),
        isLoading: false,
      );
    }
  }

  Future<AppointmentModel> getAppointmentById(String appointmentId) async {
    try {
      final appointment = await _repository.getAppointmentById(appointmentId);
      // Add to state if not already present
      if (!state.appointments.any((apt) => apt.id == appointmentId)) {
        state = state.copyWith(
          appointments: [...state.appointments, appointment],
        );
      }
      return appointment;
    } catch (e) {
      throw Exception('Failed to load appointment: $e');
    }
  }

  Future<AppointmentModel> bookAppointment({
    required String doctorId,
    required String startAt, // ISO 8601 UTC
    int slotMinutes = 30,
    String? reason,
    bool isVideo = false,
    int? serviceId,
    int? locationId,
    List<int>? documentIds,
  }) async {
    try {
      final appointment = await _repository.bookAppointment(
        doctorId: doctorId,
        startAt: startAt,
        slotMinutes: slotMinutes,
        reason: reason,
        isVideo: isVideo,
        serviceId: serviceId,
        locationId: locationId,
        documentIds: documentIds,
      );
      // Reload appointments after booking
      await loadAppointments();
      return appointment;
    } catch (e) {
      throw Exception('Failed to book appointment: $e');
    }
  }

  Future<void> cancelAppointment(String appointmentId) async {
    try {
      await _repository.cancelAppointment(appointmentId);
      // Reload appointments after cancellation
      await loadAppointments();
    } catch (e) {
      throw Exception('Failed to cancel appointment: $e');
    }
  }

  Future<Map<String, dynamic>> getAppointmentSummary(String appointmentId) async {
    return _repository.getAppointmentSummary(appointmentId);
  }

  Future<void> submitSignature(String appointmentId, String signatureImageBase64) async {
    await _repository.submitSignature(appointmentId, signatureImageBase64);
  }

  Future<Map<String, dynamic>> getPatientFormForSigning(String formId) async {
    return _repository.getPatientFormForSigning(formId);
  }

  Future<void> submitPatientFormSignature(String formId, String signatureImageBase64) async {
    await _repository.submitPatientFormSignature(formId, signatureImageBase64);
  }
}

final bookingsProvider = StateNotifierProvider<BookingsNotifier, BookingsState>((ref) {
  final repository = ref.watch(bookingsRepositoryProvider);
  return BookingsNotifier(repository);
});

/// Provides the current patient's review for an appointment, if any. Null when not yet reviewed or 404.
final appointmentReviewProvider = FutureProvider.family<AppointmentReview?, String>((ref, appointmentId) async {
  final repository = ref.watch(bookingsRepositoryProvider);
  return repository.getReviewForAppointment(appointmentId);
});
