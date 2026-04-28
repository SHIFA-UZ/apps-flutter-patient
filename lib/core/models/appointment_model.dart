import 'package:equatable/equatable.dart';
import 'doctor_model.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';

class AppointmentModel extends Equatable {
  final String id;
  final String doctorId;
  final DoctorModel? doctor;
  final String patientId;
  final String startAt;
  final String endAt;
  final String location;
  final String? reason;
  final AppointmentStatus status;
  final bool isVideo;
  final String paymentStatus;
  final int? paymentAmountMinor;
  final String? paymentCurrency;
  final bool signatureRequested;
  final bool alreadySigned;

  const AppointmentModel({
    required this.id,
    required this.doctorId,
    this.doctor,
    required this.patientId,
    required this.startAt,
    required this.endAt,
    required this.location,
    this.reason,
    required this.status,
    this.isVideo = false,
    this.paymentStatus = 'NOT_REQUIRED',
    this.paymentAmountMinor,
    this.paymentCurrency,
    this.signatureRequested = false,
    this.alreadySigned = false,
  });

  factory AppointmentModel.fromJson(Map<String, dynamic> json) {
    // Backend returns doctorId, doctorName, doctorClinic, doctorPhotoUrl
    // We need to construct a minimal DoctorModel if needed
    DoctorModel? doctor;
    if (json['doctorId'] != null) {
      final doctorName = json['doctorName']?.toString() ?? '';
      final nameParts = doctorName.split(' ');
      final firstName = nameParts.isNotEmpty ? nameParts.first : '';
      final lastName = nameParts.length > 1 ? nameParts.sublist(1).join(' ') : '';
      
      doctor = DoctorModel(
        id: json['doctorId']?.toString() ?? '',
        firstName: firstName,
        lastName: lastName,
        fullName: doctorName,
        profession: json['doctorProfession'] as String?,
        clinic: json['doctorClinic'] as String?,
        photoUrl: normalizePhotoUrl(json['doctorPhotoUrl'] as String?),
      );
    }
    
    return AppointmentModel(
      id: json['id']?.toString() ?? '',
      doctorId: json['doctorId']?.toString() ?? '',
      doctor: doctor,
      patientId: '', // Backend doesn't return patientId for patient endpoints
      startAt: json['startAt'] as String,
      endAt: json['endAt'] as String,
      location: json['location'] as String,
      reason: json['reason'] as String?,
      status: AppointmentStatus.fromString(json['status'] as String? ?? 'CONFIRMED'),
      isVideo: json['location'] == 'Video Consultation' || (json['isVideo'] as bool? ?? false),
      paymentStatus: (json['paymentStatus'] as String?) ?? 'NOT_REQUIRED',
      paymentAmountMinor: (json['paymentAmountMinor'] as num?)?.toInt(),
      paymentCurrency: json['paymentCurrency'] as String?,
      signatureRequested: json['signatureRequested'] as bool? ?? false,
      alreadySigned: json['alreadySigned'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'doctorId': doctorId,
      'patientId': patientId,
      'startAt': startAt,
      'endAt': endAt,
      'location': location,
      'reason': reason,
      'status': status.name,
      'isVideo': isVideo,
      'paymentStatus': paymentStatus,
      'paymentAmountMinor': paymentAmountMinor,
      'paymentCurrency': paymentCurrency,
    };
  }

  @override
  List<Object?> get props => [
        id,
        doctorId,
        doctor,
        patientId,
        startAt,
        endAt,
        location,
        reason,
        status,
        isVideo,
        paymentStatus,
        paymentAmountMinor,
        paymentCurrency,
        signatureRequested,
        alreadySigned,
      ];
}

enum AppointmentStatus {
  confirmed,
  cancelled,
  completed,
  pending;

  static AppointmentStatus fromString(String status) {
    return AppointmentStatus.values.firstWhere(
      (e) => e.name.toUpperCase() == status.toUpperCase(),
      orElse: () => AppointmentStatus.pending,
    );
  }
}
